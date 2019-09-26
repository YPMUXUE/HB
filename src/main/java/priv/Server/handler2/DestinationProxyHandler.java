package priv.Server.handler2;


import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutor;
import org.apache.commons.codec.language.bm.PhoneticEngine;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import priv.common.handler.EventLoggerHandler;
import priv.common.handler.ReadWriteTimeoutHandler;
import priv.common.handler2.InboundCallBackHandler;
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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Deprecated
public class DestinationProxyHandler extends ChannelDuplexHandler {

	private static final Logger logger = LoggerFactory.getLogger(DestinationProxyHandler.class);

	private Channel targetChannel;

	private boolean writeable;

	private final boolean closeTargetChannel;

	private Queue<PendingWriteItem> pendingData;
//    private static final String Proxy_Transfer_Name="DestinationProxyHandler*Transfer";

	static {

	}


	public DestinationProxyHandler() {
		this.closeTargetChannel = true;
		this.targetChannel = null;
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
			throw new RuntimeException("DestinationProxyHandler#channelRead: the msg is not a instance of Message");
		}
	}


	private void handleConnect(ChannelHandlerContext ctx, ConnectMessage m) {
		if (targetChannel == null || !targetChannel.isActive()) {
			m.release();
			ctx.channel().close().addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
		} else {
			targetChannel.writeAndFlush(m.getContent()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
		}
	}

	private void handleBindV2(ChannelHandlerContext ctx, BindV2Message m) throws Exception {
		if (!prepareConnect()){
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
			logger.error("unknown Host:"+host);
			//note: 先不主动关闭连接 等超时handler触发了再关闭
			return;
		}
		this.doConnect(ctx, address);

	}

	private void handleBindV1(ChannelHandlerContext ctx, BindV1Message m) throws Exception {
		if (!prepareConnect()){
			ReferenceCountUtil.release(m);
			return;
		}
		byte[] des = m.getDestination();
		ReferenceCountUtil.release(m);
		InetSocketAddress address = new InetSocketAddress(InetAddress.getByAddress(new byte[]{des[0], des[1], des[2], des[3]}), ((des[4] & 0xFF) << 8) | (des[5] & 0xFF));
		this.doConnect(ctx, address);
	}

	private void doConnect(ChannelHandlerContext ctx, SocketAddress address) throws Exception {
		final String socketString = address.toString();
		EventExecutor executor = ctx.executor();
		InboundCallBackHandler callBackHandler = new InboundCallBackHandler();
		callBackHandler.setChannelReadListener(new BiConsumer<ChannelHandlerContext, Object>() {
			@Override
			public void accept(ChannelHandlerContext c, Object o) {
				ctx.channel().writeAndFlush(o);
			}
		});

		callBackHandler.setExceptionCaughtListener(new BiConsumer<ChannelHandlerContext, Throwable>() {
			@Override
			public void accept(ChannelHandlerContext c, Throwable throwable) {
				logger.error(c.channel().toString(),throwable);
				c.channel().close();
			}
		});

		callBackHandler.setChannelInactiveListener(new Consumer<ChannelHandlerContext>() {
			@Override
			public void accept(ChannelHandlerContext c) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						onTargetClose(ctx,c.channel());
					}
				});
			}
		});
		ChannelInitializer<Channel> initializerToNewConnection = new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();
				pipeline.addLast("ReadWriteTimeoutHandler", new ReadWriteTimeoutHandler(StaticConfig.timeout));
//				pipeline.addLast("ConnectionToServer*transfer", new SimpleTransferHandler(ctx.channel(),false));
				pipeline.addLast("InboundCallBackHandler", callBackHandler);
				pipeline.addLast("EventLoggerHandler", new EventLoggerHandler("ProxyChannel",true));
			}
		};

		ChannelFuture connectToServer = Connections.connect(ctx.channel().eventLoop(), address, initializerToNewConnection);

		connectToServer.addListener((ChannelFutureListener) f -> {
			Runnable callback;
			Channel channelToServer = f.channel();
			if (f.isSuccess()){
				callback = new Runnable() {
					@Override
					public void run() {
						logger.info(channelToServer + " connect success");
						onTargetSuccess(ctx, channelToServer);
//						channelToServer.closeFuture().addListener((ChannelFutureListener) f->{
//							onTargetClose(ctx, f.channel());
//						});
					}
				};
			}else{
				callback = new Runnable() {
					@Override
					public void run() {
						logger.info( socketString + " connect failed");
						onTargetFailed(ctx, channelToServer);
					}
				};
			}
			ctx.executor().execute(callback);
		});

	}
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (msg instanceof ByteBuf) {
			msg = new ConnectMessage((ByteBuf) msg);
		}
		if (!this.writeable){
			pendingData.add(new PendingWriteItem(msg, promise));
		}else{
			super.write(ctx, msg, promise);
		}
	}

//	@Override
//	public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
//		if (closeTargetChannel && targetChannel != null && targetChannel.isActive()) {
//			targetChannel.eventLoop().submit(() -> {
//				targetChannel.close();
//			});
//		}
//		targetChannel = null;
//		super.close(ctx, promise);
//	}


	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		if (closeTargetChannel && targetChannel != null && targetChannel.isActive()) {
			removeTargetChannel(closeTargetChannel);
		}
		targetChannel = null;
		super.channelInactive(ctx);
	}

	private void writePendingData(ChannelHandlerContext ctx){
		Queue<PendingWriteItem> pendingData = this.pendingData;
		if (CollectionUtils.isNotEmpty(pendingData)) {
			while (!pendingData.isEmpty()){
				PendingWriteItem item = pendingData.poll();
				if (item != null) {
					ctx.write(item.data, item.promise);
				}
			}
			ctx.flush();
		}
	}

	private boolean prepareConnect() {
		removeTargetChannel(true);
		this.pendingData = new LinkedList<>();
		return true;
	}

	private void removeTargetChannel(boolean closeTargetChannel){
		Channel tmpChannel = this.targetChannel;
		this.targetChannel = null;
		this.writeable = false;
		if (tmpChannel != null && closeTargetChannel) {
			tmpChannel.eventLoop().submit(() -> {
				if (tmpChannel.isActive())
					tmpChannel.close();
			});
		}
	}

	private void onTargetFailed(ChannelHandlerContext ctx, Channel channelToServer) {
		ctx.writeAndFlush(new ConnectionEstablishFailedMessage()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
		removeTargetChannel(false);
	}
	private void onTargetSuccess(ChannelHandlerContext ctx, Channel targetChannel){
		this.targetChannel = targetChannel;
		ctx.writeAndFlush(new ConnectionEstablishMessage()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
		writePendingData(ctx);
		this.writeable = true;
		this.pendingData = null;
	}
	private void onTargetClose(ChannelHandlerContext ctx, Channel targetChannel){
		if (Objects.equals(targetChannel,this.targetChannel)){
			logger.info("target channel closed."+targetChannel.toString());
			removeTargetChannel(false);
			if(ctx.channel().isActive()) {
				ctx.writeAndFlush(new CloseMessage()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
			}
		}
	}

	private static class PendingWriteItem {
		private Object data;
		private ChannelPromise promise;

		private PendingWriteItem(Object data, ChannelPromise promise) {
			this.data = data;
			this.promise = promise;
		}
	}
}
