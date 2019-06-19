package priv.Server.handler2;


import priv.common.handler.ReadWriteTimeoutHandler;
import priv.common.handler.SimpleTransferHandler;
import priv.common.log.LogUtil;
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
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.concurrent.EventExecutor;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

public class DestinationProxyHandler extends ChannelDuplexHandler {


	private Channel targetChannel;
	private final boolean closeTargetChannel;
//    private static final String Proxy_Transfer_Name="DestinationProxyHandler*Transfer";

	static {

	}

	public DestinationProxyHandler() {
		this.closeTargetChannel = false;
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
			ctx.channel().close().addListener(LogUtil.LOGGER_ON_FAILED_CLOSE);
			m.release();
		} else {
			targetChannel.writeAndFlush(m.getContent()).addListener(LogUtil.LOGGER_ON_FAILED_CLOSE);
		}
	}

	private void handleBindV2(ChannelHandlerContext ctx, BindV2Message m) throws Exception {
		removeOldConnection(ctx, m);
		String host = m.getHostName();
		int port = m.getPort();
		InetSocketAddress address;
		try {
			address = new InetSocketAddress(InetAddress.getByName(host), port);
		} catch (UnknownHostException e) {
			ctx.writeAndFlush(new ConnectionEstablishFailedMessage("unknown Host:" + host));
			//note: 先不主动关闭连接 等超时handler触发了再关闭
			return;
		}
		this.doConnect(ctx, address);

	}

	private void handleBindV1(ChannelHandlerContext ctx, BindV1Message m) throws Exception {
		removeOldConnection(ctx, m);
		byte[] des = m.getDestination();
		InetSocketAddress address = new InetSocketAddress(InetAddress.getByAddress(new byte[]{des[0], des[1], des[2], des[3]}), ((des[4] & 0xFF) << 8) | (des[5] & 0xFF));
		this.doConnect(ctx, address);
	}

	private void doConnect(ChannelHandlerContext ctx, SocketAddress address) throws Exception {

		ChannelInitializer<Channel> initializerToNewConnection = new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();
				pipeline.addLast("ReadWriteTimeoutHandler", new ReadWriteTimeoutHandler(StaticConfig.timeout));
				pipeline.addLast("ConnectionToServer*transfer", new SimpleTransferHandler(ctx.channel()));
			}
		};
		EventExecutor executor = ctx.executor();

		ChannelFuture connectToServer = Connections.connect(ctx.channel().eventLoop(), address, initializerToNewConnection);
		Channel channelToServer = connectToServer.channel();
		connectToServer.addListener((f) -> {
			if (f.isSuccess()) {
				Runnable task = () -> {
					DestinationProxyHandler.this.targetChannel = channelToServer;

					channelToServer.closeFuture().addListener((closeFuture)->{
						Runnable closeTask = ()->{
							if (channelToServer.equals(DestinationProxyHandler.this.targetChannel)){
								LogUtil.info(()->"server close the connection"+channelToServer);
								DestinationProxyHandler.this.targetChannel = null;
								ctx.channel().writeAndFlush(new CloseMessage());
							}
						};
						if (executor.inEventLoop()){
							closeTask.run();
						}else{
							executor.execute(closeTask);
						}
					});
					LogUtil.info(() -> channelToServer + " connect success");
					ctx.writeAndFlush(new ConnectionEstablishMessage()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
				};
				if (executor.inEventLoop()){
					task.run();
				}else {
					executor.execute(task);
				}
			} else {
				LogUtil.info(() -> channelToServer + " connect failed");
				ctx.writeAndFlush(new ConnectionEstablishFailedMessage()).addListener(ChannelFutureListener.CLOSE);
			}
		});
	}

	private void removeOldConnection(ChannelHandlerContext ctx, Message m) {
		if (this.targetChannel != null) {
			this.targetChannel.eventLoop().submit(() -> {
				if (targetChannel.isActive())
					targetChannel.close();
			});
			this.targetChannel = null;
		}
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (msg instanceof ByteBuf) {
			msg = new ConnectMessage((ByteBuf) msg);
		}
		super.write(ctx, msg, promise);
	}

	@Override
	public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
		if (closeTargetChannel && targetChannel != null && targetChannel.isActive()) {
			targetChannel.eventLoop().submit(() -> {
				targetChannel.close();
			});
		}
		super.close(ctx, promise);
	}
}
