package Server.v2;


import common.handler.SimpleTransferHandler;
import common.log.LogUtil;
import common.message.frame.Message;
import common.message.frame.bind.BindV1Message;
import common.message.frame.bind.BindV2Message;
import common.message.frame.connect.ConnectMessage;
import common.message.frame.establish.ConnectionEstablishFailedMessage;
import common.message.frame.establish.ConnectionEstablishMessage;
import common.message.frame.login.LoginMessage;
import common.resource.ConnectionEvents;
import common.resource.SystemConfig;
import common.util.Connections;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

public class DestinationProxyHandler extends ChannelDuplexHandler {

	private Channel targetChannel;
	private final boolean closeTargetChannel;
//    private static final String Proxy_Transfer_Name="DestinationProxyHandler*Transfer";

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
			address = new InetSocketAddress(InetAddress.getByName(host),port);
		} catch (UnknownHostException e) {
			ctx.writeAndFlush(new ConnectionEstablishFailedMessage("unknown Host:"+host));
			//todo 先不主动关闭连接 等超时handler触发了再关闭
			return;
		}
		this.targetChannel = this.getConnection(ctx, address);

	}

	private void handleBindV1(ChannelHandlerContext ctx, BindV1Message m) throws Exception {
		removeOldConnection(ctx, m);
		byte[] des = m.getDestination();
		InetSocketAddress address = new InetSocketAddress(InetAddress.getByAddress(new byte[]{des[0], des[1], des[2], des[3]}), ((des[4] & 0xFF) << 8) | (des[5] & 0xFF));
		this.targetChannel = this.getConnection(ctx, address);
	}

	private Channel getConnection(ChannelHandlerContext ctx, SocketAddress address)throws Exception{
		ChannelFuture channelFuture = Connections.newConnectionToServer(ctx.channel().eventLoop()
				, address
				, (status, channelToServer) -> {
					if (status == SystemConfig.SUCCESS) {
						channelToServer.pipeline().addLast("ConnectionToServer*transfer", new SimpleTransferHandler(ctx.channel()));

						LogUtil.info(() -> channelToServer + " connect success");
//                        ctx.pipeline().addAfter(ctx.name(),Proxy_Transfer_Name,new SimpleTransferHandler(channelToServer,true));
						ctx.writeAndFlush(new ConnectionEstablishMessage()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
					} else {
						LogUtil.info(() -> channelToServer + " connect failed");
						ctx.writeAndFlush(new ConnectionEstablishFailedMessage())
								.addListener(ChannelFutureListener.CLOSE);
					}
				});
		return channelFuture.channel();
	}

	private void removeOldConnection(ChannelHandlerContext ctx, Message m) {
		if (this.targetChannel != null) {
			this.targetChannel.eventLoop().submit(()->{
				if(targetChannel.isActive())
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
