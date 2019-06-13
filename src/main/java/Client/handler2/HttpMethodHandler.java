package Client.handler2;

import Client.bean.HostAndPort;
import common.Message;
import common.handler.EventLoggerHandler;
import common.handler.SimpleTransferHandler;
import common.handler2.coder.AllMessageTransferHandler;
import common.log.LogUtil;
import common.resource.StaticConfig;
import common.resource.SystemConfig;
import common.util.Connections;
import common.util.HandlerHelper;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutor;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

public class HttpMethodHandler extends ChannelInboundHandlerAdapter {
	private final static String clientTransferHandler = "ClientTransferHandler";
	private final static String proxyTransferHandler = "ProxyTransferHandler";
	private Channel channelToProxyServer;

	public HttpMethodHandler() {
	}


	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (!(msg instanceof FullHttpRequest)) {
			ctx.fireChannelRead(msg);
		} else {
			FullHttpRequest m = (FullHttpRequest) msg;
			LogUtil.debug(m::toString);
			try {
				if (HttpMethod.CONNECT.equals(m.method())) {
					handleConnect(ctx, m);
				} else {
					//TODO 以后有心情了再考虑
//                handleSimpleProxy(ctx, m);
					ctx.channel().close();
					return;
				}
			} catch (Throwable e) {
				LogUtil.info(() -> LogUtil.stackTraceToString(e));
			} finally {
				ReferenceCountUtil.release(msg);
			}
		}
	}

	private void handleSimpleProxy(ChannelHandlerContext ctx, FullHttpRequest msg) {
		throw new UnsupportedOperationException("普通HTTP代理请求还没完成，先放着");
	}

	private void handleConnect(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
		final String hostName = msg.uri();
		final HostAndPort destination = HostAndPort.resolve(msg);
		ChannelInitializer channelInitializer = new ChannelInitializer() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				//inbound
				ch.pipeline().addLast("LengthFieldBasedFrameDecoder", HandlerHelper.newDefaultFrameDecoderInstance());
				//消息转换
				ch.pipeline().addLast("MessageTransferHandler", new AllMessageTransferHandler());
				//inbound消息转发
				ch.pipeline().addLast("SimpleTransferHandler", new SimpleTransferHandler(ctx.channel(), false));
				//log
				ch.pipeline().addLast("EventLoggerHandler",new EventLoggerHandler(false));
			}
		};
		ChannelFuture future = Connections.connect(ctx.channel().eventLoop(), getProxyAddress(), channelInitializer);
		Channel channel = future.channel();
		EventExecutor executor = ctx.executor();
		future.addListener((f)->{
			if (f.isSuccess()){
				Runnable task = ()->{
					//删除所有RequestToClient下ChannelHandler
					ctx.pipeline().forEach((entry) -> ctx.pipeline().remove(entry.getKey()));
					ctx.pipeline().addLast("ReadTimeoutHandler", new ReadTimeoutHandler(SystemConfig.timeout, TimeUnit.SECONDS))
                        .addLast(clientTransferHandler, new ClientTransferHandler(channelToProxyServer, true, ClientTransferHandler.HTTPS_PROCESSOR))
                        .addLast("EventLoggerHandler", new EventLoggerHandler((context,cause)->"ClientToProxyServer:" + EventLoggerHandler.DEFAULT_HANDLER.apply(context,cause)));
//
				}
			}
		});

//        Connections.newConnectionToProxyServer(ctx.channel().eventLoop(), (channelFuture, channelToProxyServer) -> {
//            if (channelFuture.isSuccess()) {
//                LogUtil.info(() -> ("Proxy Server:" + StaticConfig.PROXY_SERVER_ADDRESS + " connect success, bind address" + hostName));
//
//                //删除所有RequestToClient下ChannelHandler
//                ctx.pipeline().forEach((entry) -> ctx.pipeline().remove(entry.getKey()));
//                ctx.pipeline().addLast("ReadTimeoutHandler", new ReadTimeoutHandler(SystemConfig.timeout, TimeUnit.SECONDS))
//                        .addLast(clientTransferHandler, new ClientTransferHandler(channelToProxyServer, true, ClientTransferHandler.HTTPS_PROCESSOR))
//                        .addLast("EventLoggerHandler", new EventLoggerHandler((context,cause)->"ClientToProxyServer:" + EventLoggerHandler.DEFAULT_HANDLER.apply(context,cause)));
//
//                channelToProxyServer.pipeline().addLast(proxyTransferHandler, new ProxyTransferHandler(ctx.channel(), true, destination));
//            } else {
//                LogUtil.info(() -> (hostName + "connect failed"));
//                ctx.channel().close();
//            }
//        });
	}

	private Message PackageMessageToProxyServer(FullHttpRequest req) {
		return null;
	}

	private SocketAddress getProxyAddress() throws Exception {
		InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(StaticConfig.PROXY_SERVER_ADDRESS), StaticConfig.PROXY_SERVER_PORT);
		return address;
	}
}
