package priv.Server.handler2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import io.netty.util.concurrent.EventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import priv.common.handler.EventLoggerHandler;
import priv.common.handler.ReadWriteTimeoutHandler;
import priv.common.handler2.InboundCallBackHandler;
import priv.common.log.LogUtil;
import priv.common.message.ChannelDataEntry;
import priv.common.message.frame.Message;
import priv.common.message.frame.bind.BindV1Message;
import priv.common.message.frame.bind.BindV2Message;
import priv.common.message.frame.close.CloseMessage;
import priv.common.message.frame.connect.ConnectMessage;
import priv.common.message.frame.establish.ConnectionEstablishFailedMessage;
import priv.common.message.frame.establish.ConnectionEstablishMessage;
import priv.common.message.frame.login.LoginMessage;
import priv.common.resource.ConnectionEvents;
import priv.common.resource.StaticConfig;
import priv.common.util.Connections;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 *  * @author  pyuan
 *  * @date    2019/9/4 0004
 *  * @Description
 *  *
 *  
 */
public class MessageServerHandler extends ChannelDuplexHandler {
	private static final Logger logger = LoggerFactory.getLogger(MessageServerHandler.class);

	private ChannelFuture targetChannelFuture;

	private final boolean closeTargetChannel;


	public MessageServerHandler() {
		this.closeTargetChannel = true;
	}


	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof Message) {
			Message m = (Message) msg;
			ConnectionEvents connectionEvent = m.supportConnectionEvent();

			if (m instanceof BindV1Message) {
				handleBindV1(ctx, (BindV1Message) m);
			} else if (m instanceof BindV2Message) {
				handleBindV2(ctx, (BindV2Message) m);
			} else if (m instanceof ConnectMessage) {
				handleConnect(ctx, (ConnectMessage) m);
			} else if (m instanceof LoginMessage) {
				throw new UnsupportedOperationException();
			} else {
				throw new Exception("a unsupported header :" + connectionEvent);
			}
		} else {
			throw new RuntimeException("MessageServerHandler#channelRead: the msg is not a instance of Message");
		}
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (msg instanceof ChannelDataEntry) {
			ChannelDataEntry<? extends ReferenceCounted> m = (ChannelDataEntry<? extends ReferenceCounted>) msg;
			Channel channel = m.getBindChannel();
			ReferenceCounted data = m.getData();
			if (this.targetChannelFuture != null && Objects.equals(this.targetChannelFuture.channel(), channel)) {
				msg = new ConnectMessage((ByteBuf) data);
			}else{
				ReferenceCountUtil.release(data);
				Channel targetChannel = null;
				if (this.targetChannelFuture != null) {
					targetChannel = this.targetChannelFuture.channel();
				}
				promise.tryFailure(new IOException("targetChannelFuture is null or channel is reset by others"
						+ (targetChannel == null ? "" : targetChannel.toString())));
				return;
			}
		}
		super.write(ctx, msg, promise);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ChannelFuture f = this.targetChannelFuture;
		if (f != null) {
			removeTargetChannel(closeTargetChannel);
			this.targetChannelFuture = null;
		}
		super.channelInactive(ctx);
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof ProxyEvent) {
			ProxyEvent event = (ProxyEvent) evt;
			if (event.type == ProxyEvent.BIND_SUCCESS) {
				onBindSuccess(ctx, event.channelFuture.channel());
			} else if (event.type == ProxyEvent.BIND_FAILED) {
				onBindFailed(ctx, event.channelFuture.channel());
			} else if (event.type == ProxyEvent.TARGET_INACTIVE) {
				onTargetClose(ctx, event.channelFuture.channel());
			} else {
				throw new UnsupportedOperationException("wrong type of ProxyEvent:" + event.type);
			}
		}
		super.userEventTriggered(ctx, evt);
	}

	private void handleConnect(ChannelHandlerContext ctx, ConnectMessage m) {
		if (this.targetChannelFuture == null) {
			ReferenceCountUtil.release(m);
			ctx.channel().writeAndFlush(new CloseMessage());
			return;
		}

		ByteBuf content = m.getContent();
		if (!this.targetChannelFuture.isDone()) {
			this.targetChannelFuture.addListener((ChannelFutureListener) f -> {
				if (f.isSuccess()){
					f.channel().writeAndFlush(content);
				}else{
					ReferenceCountUtil.release(content);
				}
			});
			return;
		}

		Channel targetChannel = this.targetChannelFuture.channel();
		if (targetChannel == null || !targetChannel.isActive()) {
			ReferenceCountUtil.release(m);
		} else {
			targetChannel.writeAndFlush(content);
		}
	}

	private void handleBindV2(ChannelHandlerContext ctx, BindV2Message m) throws Exception {
		if (!prepareConnect()) {
			ReferenceCountUtil.release(m);
			return;
		}
		String host = m.getHostName();
		int port = m.getPort();
		ReferenceCountUtil.release(m);
		InetSocketAddress address;
		try {
			address = new InetSocketAddress(InetAddress.getByName(host), port);
		} catch (UnknownHostException e) {
			ctx.writeAndFlush(new ConnectionEstablishFailedMessage("unknown Host:" + host));
			logger.error("unknown Host:" + host);
			//note: 不主动关闭连接
			return;
		}
		this.connectRemoteAddress(ctx, address);

	}

	private void handleBindV1(ChannelHandlerContext ctx, BindV1Message m) throws Exception {
		if (!prepareConnect()) {
			ReferenceCountUtil.release(m);
			return;
		}
		byte[] des = m.getDestination();
		ReferenceCountUtil.release(m);
		InetSocketAddress address = new InetSocketAddress(InetAddress.getByAddress(new byte[]{des[0], des[1], des[2], des[3]}), ((des[4] & 0xFF) << 8) | (des[5] & 0xFF));
		this.connectRemoteAddress(ctx, address);
	}

	private void connectRemoteAddress(ChannelHandlerContext ctx, SocketAddress address) throws Exception {
		final String socketString = address.toString();
		EventExecutor executor = ctx.executor();
		InboundCallBackHandler callBackHandler = new InboundCallBackHandler();
		callBackHandler.setChannelReadListener(new BiConsumer<ChannelHandlerContext, Object>() {
			@Override
			public void accept(ChannelHandlerContext c, Object o) {
				ctx.channel().writeAndFlush(new ChannelDataEntry<ByteBuf>(c.channel(), (ByteBuf) o));
			}
		});

		callBackHandler.setChannelInactiveListener(new Consumer<ChannelHandlerContext>() {
			@Override
			public void accept(ChannelHandlerContext c) {
				final ProxyEvent event = new ProxyEvent(ProxyEvent.TARGET_INACTIVE, c.channel().newSucceededFuture());
				executor.execute(new Runnable() {
					@Override
					public void run() {
						ctx.pipeline().fireUserEventTriggered(event);
					}
				});
			}
		});
		ChannelInitializer<Channel> initializerToNewConnection = new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();
				pipeline.addLast("ReadWriteTimeoutHandler", new ReadWriteTimeoutHandler(StaticConfig.timeout));
				pipeline.addLast("InboundCallBackHandler", callBackHandler);
				pipeline.addLast("EventLoggerHandler", new EventLoggerHandler("ProxyChannel"));
			}
		};

		ChannelFuture connectToServer = Connections.connect(ctx.channel().eventLoop(), address, initializerToNewConnection);
		this.targetChannelFuture = connectToServer;

		connectToServer.addListener((ChannelFutureListener) f -> {
			ProxyEvent event;
			if (f.isSuccess()) {
				event = new ProxyEvent(ProxyEvent.BIND_SUCCESS, f);
			} else {
				logger.error("connection:{},address:{},bind failed.{}", f.channel().toString(), socketString, f.cause() == null ? "" : LogUtil.stackTraceToString(f.cause()));
				event = new ProxyEvent(ProxyEvent.BIND_FAILED, f);
			}
			ctx.pipeline().fireUserEventTriggered(event);
		});
	}

	@Override
	public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
		removeTargetChannel(true);
		super.close(ctx, promise);
	}

	private boolean prepareConnect() {
		if (this.targetChannelFuture != null && !targetChannelFuture.isDone()) {
			return false;
		} else {
			removeTargetChannel(true);
			return true;
		}
	}

	private void removeTargetChannel(boolean closeTargetChannel) {
		final ChannelFuture targetFuture = this.targetChannelFuture;
		this.targetChannelFuture = null;
		if (targetFuture == null) {
			return;
		}
		if (closeTargetChannel) {
			if (!targetFuture.isDone()) {
				targetFuture.addListener((ChannelFutureListener) (f) -> {
					if (f.isSuccess()) {
						f.channel().close();
					}
				});
			} else {
				Channel targetChannel = targetFuture.channel();
				if (targetChannel != null) {
					targetChannel.eventLoop().submit(() -> {
						if (targetChannel.isActive()) {
							targetChannel.close();
						}
					});
				}
			}
		}
	}

	private void onBindFailed(ChannelHandlerContext ctx, Channel targetChannel) {
		if (this.targetChannelFuture == null) {
			return;
		}
		if (!this.targetChannelFuture.channel().equals(targetChannel)) {
			return;
		}

		ctx.writeAndFlush(new ConnectionEstablishFailedMessage()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
		removeTargetChannel(false);
	}

	private void onBindSuccess(ChannelHandlerContext ctx, Channel targetChannel) {
		if (this.targetChannelFuture == null) {
			targetChannel.eventLoop().execute(() -> targetChannel.close());
			return;
		}
		if (!this.targetChannelFuture.channel().equals(targetChannel)) {
			targetChannel.eventLoop().execute(() -> targetChannel.close());
			return;
		}
		ctx.writeAndFlush(new ConnectionEstablishMessage()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
	}

	private void onTargetClose(ChannelHandlerContext ctx, Channel targetChannel) {
		if (this.targetChannelFuture == null) {
			return;
		}
		if (!Objects.equals(this.targetChannelFuture.channel(), targetChannel)) {
			return;
		}

		logger.info("target channel inactive." + targetChannel.toString());
		removeTargetChannel(false);
		ctx.writeAndFlush(new CloseMessage()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
	}


	private static class ProxyEvent {
		private static final int BIND_SUCCESS = 0;
		private static final int BIND_FAILED = 1;
		private static final int TARGET_INACTIVE = 2;

		private final int type;
		private final ChannelFuture channelFuture;

		ProxyEvent(int type, ChannelFuture channelFuture) {
			this.type = type;
			this.channelFuture = channelFuture;
		}
	}
}
