package priv.common.connection;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
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


	private final ByteBufCache outputCache = new ByteBufCache();
	private final Consumer<Events> listener;
	private final int capacityFlag;
	private final ChannelFuture connectFuture;
//	private final EventLoop eventLoop;
//	private final SocketAddress targetSocketAddr;
	//should not set a new value after being set a object from null
	private final AtomicReference<ChannelPromise> bindPromiseRef = new AtomicReference<>(null);
	private final AtomicInteger statusMask = new AtomicInteger(0);

//	private volatile ChannelFuture connectFuture;

	public ProxyRemoteConnection(ChannelFuture connectFuture, int capacityFlag, Consumer<Events> listener) {
		this.capacityFlag = capacityFlag;
		this.listener = Objects.requireNonNull(listener);
		this.connectFuture = connectFuture;
	}

	public static ChannelInitializer<Channel> getPreCacheHandler() {
		return new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				ch.pipeline().addFirst(new ChannelInboundHandlerAdapter(){
					@Override
					public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
						super.channelRead(ctx, msg);
					}
				})
			}
		};
	}

	private static ChannelInitializer<Channel> getProxyChannelInitializer(final RemoteDataHandler<Message> dataHandler) {
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
			}
		});

		ChannelInitializer<Channel> channelInitializer = new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				byte[] aesKey = Base64.decodeBase64(StaticConfig.AES_KEY);
				ChannelPipeline pipeline = ch.pipeline();
				pipeline.addLast("AESHandler", new AesEcbCryptHandler(aesKey));
				pipeline.addLast(HandlerHelper.newDefaultFrameDecoderInstance());
				pipeline.addLast(new AllMessageTransferHandler());
//				pipeline.addLast(new HttpProxyMessageHandler());
				pipeline.addLast(callBack);
				pipeline.addLast(new EventLoggerHandler("Proxy remote channel"));
			}
		};
		return channelInitializer;
	}

	public static Consumer<Events> newDefaultListener(final Channel notifyChannel) {
		return new Consumer<Events>() {
			@Override
			public void accept(Events events) {
				notifyChannel.pipeline().fireUserEventTriggered(events);
			}
		};
	}

	public ChannelFuture bind(HostAndPort hostAndPort) {
		if (this.bindPromiseRef.get() != null) {
			throw new IllegalStateException("the bind is modify by others");
		}
		ChannelFuture connectFuture = Connections.connect(this.eventLoop, this.targetSocketAddr, getProxyChannelInitializer(this));
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
					boolean result = statusMask.compareAndSet(STATUS_INIT, STATUS_CONNECT);
					if (!result){
						IllegalStateException e = new IllegalStateException("the channel connect scccess but status is not in 'init'");
						bindPromise.setFailure(e);
						throw e;
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
			result = this.outputBuffer;
			this.outputBuffer = null;
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
		ChannelPromise writePromise = bindPromise.channel().newPromise();
		Message connectMessage = new ConnectMessage(data);
		bindPromise.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				future.channel().writeAndFlush(connectMessage, writePromise);
			}
		});
		return writePromise;
	}

	public void addBufferCache(ByteBuf data) {
		if (isClose() || data == null || data.readableBytes() <= 0) {
			ReferenceCountUtil.release(data);
			return;
		}
		this.outputCache.writeBuffer(data);
		listener.accept(Events.OP_READ);
	}

	public boolean isClose(){
		return this.statusMask.get() == STATUS_CLOSE;
	}

	@Override
	public ChannelFuture close() {
		ChannelPromise bindPromise = this.bindPromiseRef.get();
		if (bindPromise == null){
			return null;
		}

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

	private void onReceiveConnectionEstablish(ConnectionEstablishMessage data) {
		this.bindFuture.setSuccess();
	}
}
