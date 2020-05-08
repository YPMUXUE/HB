package priv.common.connection;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.codec.binary.Base64;
import priv.Client.bean.HostAndPort;
import priv.common.handler.EventLoggerHandler;
import priv.common.handler2.AesEcbCryptHandler;
import priv.common.handler2.InboundCallBackHandler;
import priv.common.handler2.coder.AllMessageTransferHandler;
import priv.common.message.frame.Message;
import priv.common.message.frame.bind.BindV2Message;
import priv.common.message.frame.close.CloseMessage;
import priv.common.message.frame.connect.ConnectMessage;
import priv.common.message.frame.establish.ConnectionEstablishFailedMessage;
import priv.common.message.frame.establish.ConnectionEstablishMessage;
import priv.common.resource.ConnectionEvents;
import priv.common.resource.StaticConfig;
import priv.common.util.Connections;
import priv.common.util.HandlerHelper;

import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 *  * @author  pyuan
 *  * @date    2020/3/6 0006
 *  * @Description
 *  *
 *  
 */
public class ProxyRemoteConnection implements Remote<ByteBuf>, RemoteDataHandler<Message> {
	public static final int STATUS_INIT = 0;

	public static final int STATUS_CONNECT = 1;

	public static final int STATUS_BIND = 2;

	public static final int STATUS_CLOSE = 3;

	private static final ChannelHandler replaceChannelHandlerFlag = new ReplaceChannelHandlerFlag();


	private final ByteBufCache outputCache = new ByteBufCache();
	private final Consumer<Events> listener;
	private final int capacityFlag;
	private final ChannelFuture connectFuture;
	private final ChannelFuture closeFuture;
	private final AtomicReference<ChannelPromise> bindPromiseRef = new AtomicReference<>(null);
	private final AtomicInteger status = new AtomicInteger(STATUS_INIT);

	private ProxyRemoteConnection(ChannelFuture connectFuture, int capacityFlag, Consumer<Events> listener) {
		this.capacityFlag = capacityFlag;
		this.listener = Objects.requireNonNull(listener);
		this.connectFuture = connectFuture;
		this.closeFuture = this.connectFuture.channel().newPromise();
	}

	public static ProxyRemoteConnection build(EventLoop eventLoop, SocketAddress targetSocketAddr, int capacityFlag, Consumer<Events> listener) {
		ChannelInitializer<Channel> channelInitializer = new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				byte[] aesKey = Base64.decodeBase64(StaticConfig.AES_KEY);
				ChannelPipeline pipeline = ch.pipeline();
				pipeline.addLast("AESHandler", new AesEcbCryptHandler(aesKey));
				pipeline.addLast("LengthFieldBasedFrameDecoder", HandlerHelper.newDefaultFrameDecoderInstance());
				pipeline.addLast("MessageTransferHandler", new AllMessageTransferHandler());
//				pipeline.addLast(new HttpProxyMessageHandler());
				pipeline.addLast(replaceChannelHandlerFlag);
				pipeline.addLast(new EventLoggerHandler("Proxy remote channel"));
			}
		};

		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(Objects.requireNonNull(eventLoop))
				.channel(NioSocketChannel.class)
				.handler(channelInitializer)
				.option(ChannelOption.AUTO_READ, false);
		ChannelFuture connectFuture = bootstrap.connect(targetSocketAddr);

		return new ProxyRemoteConnection(connectFuture, capacityFlag, listener);

	}

	private static ChannelFuture HandleReplaceFlag(ChannelFuture connectFuture, RemoteDataHandler<Message> dataHandler) {
		InboundCallBackHandler callBack = new InboundCallBackHandler();
		callBack.setChannelReadListener(new BiConsumer<ChannelHandlerContext, Object>() {
			@Override
			public void accept(ChannelHandlerContext c, Object o) {
				if (o instanceof Message) {
					dataHandler.receiveData((Message) o);
				} else {
					throw new IllegalArgumentException("unknow object:" + o.toString());
				}
			}
		});

		callBack.setChannelInactiveListener(new Consumer<ChannelHandlerContext>() {
			@Override
			public void accept(ChannelHandlerContext c) {
				dataHandler.receiveData(new CloseMessage());
				c.fireChannelInactive();
			}
		});

		Channel channel = connectFuture.channel();
		channel.pipeline().replace(replaceChannelHandlerFlag, "callBackHandler", callBack);
		channel.config().setAutoRead(true);
		return connectFuture;
	}

	public ChannelFuture bind(HostAndPort hostAndPort) {
		if (this.bindPromiseRef.get() != null) {
			throw new IllegalStateException("the bind is modify by others");
		}
		ChannelFuture connectFuture = HandleReplaceFlag(this.connectFuture, this);
		ChannelPromise bindPromise = connectFuture.channel().newPromise();
		boolean result = this.bindPromiseRef.compareAndSet(null, bindPromise);
		if (!result) {
			Connections.close(connectFuture.channel());
			throw new IllegalStateException("the bind is modify by others");
		}
		Message initMessage = bindMessage(hostAndPort);
		connectFuture.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					boolean result = status.compareAndSet(STATUS_INIT, STATUS_CONNECT);
					if (!result) {
						IllegalStateException e = new IllegalStateException("the channel connect scccess but status is not in 'init'");
						bindPromise.setFailure(e);
						future.channel().pipeline().fireExceptionCaught(e);
						return;
					}
					future.channel().writeAndFlush(initMessage).addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							if (!future.isSuccess()) {
								bindPromise.setFailure(future.cause());
							}
						}
					});
				} else {
					bindPromise.setFailure(future.cause());
				}
			}
		});
		return bindPromise;
	}

	private Message bindMessage(HostAndPort hostAndPort) {
		return new BindV2Message(hostAndPort.getHostString(), hostAndPort.getPort());
	}

	@Override
	public ByteBuf get() {
		ByteBuf result;
		synchronized (this) {
			result = outputCache.get();
		}
		return result;
	}

	@Override
	public ChannelFuture write(ByteBuf data) {
		ChannelPromise bindPromise = this.bindPromiseRef.get();
		if (bindPromise == null) {
			ReferenceCountUtil.release(data);
			throw new NullPointerException("the bind channel is null");
		}
		boolean bindSuccess = bindPromise.isSuccess();
		if (!bindSuccess) {
			throw new RuntimeException("the bind is not complete or failed");
		}
		Channel targetChannel = bindPromise.channel();
		Message connectMessage = new ConnectMessage(data);
		ChannelPromise writePromise = targetChannel.newPromise();
		targetChannel.writeAndFlush(connectMessage, writePromise);

		return writePromise;

	}

	public void addBufferCache(ByteBuf data) {
		if (isClose() || data == null || data.readableBytes() <= 0) {
			ReferenceCountUtil.release(data);
			return;
		}
		synchronized (this) {
			outputCache.writeBuffer(data);
		}
	}

	public boolean isClose() {
		return this.status.get() == STATUS_CLOSE;
	}

	public ChannelFuture close() {
		status.set(STATUS_CLOSE);
		disableListener();
		Channel targetChannel = connectFuture.channel();
		ChannelPromise closePromise = targetChannel.newPromise();
		Connections.close(targetChannel);
		synchronized (this) {
			outputCache.clear();
		}
		return closePromise;
	}

	@Override
	public void receiveData(Message data) {
		ConnectionEvents events = data.supportConnectionEvent();
		switch (events) {
			case CONNECT:
				this.addBufferCache(((ConnectMessage) data).getContent());
				break;
			case CLOSE:
				this.onReceiveCloseMessage((CloseMessage) data);
				break;
			case CONNECTION_ESTABLISH_FAILED:
				this.onReceiveConnectionEstablishFailed((ConnectionEstablishFailedMessage) data);
				break;
			case CONNECTION_ESTABLISH:
				this.onReceiveConnectionEstablish((ConnectionEstablishMessage) data);
				break;
			default:
				throw new IllegalArgumentException("unknown events" + data.toString());
		}
	}

	private void onReceiveConnectionEstablishFailed(ConnectionEstablishFailedMessage data) {

	}

	private void onReceiveCloseMessage(CloseMessage data) {

	}

	public EventLoop eventLoop() {
		return connectFuture.channel().eventLoop();
	}

	private void invokeLater(Runnable r) {
		eventLoop().execute(r);
	}

	private void onReceiveConnectionEstablish(ConnectionEstablishMessage data) {
		this.bindPromiseRef.get().setSuccess();
	}

	@ChannelHandler.Sharable
	private static class ReplaceChannelHandlerFlag extends ChannelInboundHandlerAdapter {
		@Override
		public boolean isSharable() {
			return true;
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			throw new UnsupportedOperationException("should not fire events while this handler is in the pipeline");
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
			throw new UnsupportedOperationException("should not fire events while this handler is in the pipeline");
		}
	}
}
