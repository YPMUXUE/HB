package priv.Client.handler2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import priv.Client.bean.HostAndPort;
import priv.common.handler.EventLoggerHandler;
import priv.common.handler.SimpleTransferHandler;
import priv.common.handler2.coder.AllMessageTransferHandler;
import priv.common.log.LogUtil;
import priv.common.message.frame.Message;
import priv.common.message.frame.bind.BindV2Message;
import priv.common.resource.StaticConfig;
import priv.common.util.Connections;
import priv.common.util.HandlerHelper;
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

	private static final Logger logger = LoggerFactory.getLogger(HttpMethodHandler.class);
	public HttpMethodHandler() {
	}


	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		try {
			if (msg instanceof FullHttpRequest) {
				FullHttpRequest m = (FullHttpRequest) msg;
				logger.debug(m.toString());

				if (HttpMethod.CONNECT.equals(m.method())) {
					handleConnect(ctx, m);
				} else {
					//TODO
//                handleSimpleProxy(ctx, m);
					ctx.channel().close();
					return;
				}
			}
		} catch (Throwable e) {
			logger.error(LogUtil.stackTraceToString(e));
		} finally {
			ReferenceCountUtil.release(msg);
		}

	}

	private void handleSimpleProxy(ChannelHandlerContext ctx, FullHttpRequest msg) {
		throw new UnsupportedOperationException("普通HTTP代理请求还没完成，先放着");
	}

	private void handleConnect(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
		final String hostName = msg.uri();
		final HostAndPort destination = HostAndPort.resolve(msg);
		logger.debug(destination.toString());
		Message bindMessage = new BindV2Message(destination.getHostString(),destination.getPort());
		ChannelInitializer channelInitializer = new ChannelInitializer() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				//inbound
				ch.pipeline().addLast("LengthFieldBasedFrameDecoder", HandlerHelper.newDefaultFrameDecoderInstance());
				//消息转换
				ch.pipeline().addLast("MessageTransferHandler", new AllMessageTransferHandler());
				//inbound消息转发
				ch.pipeline().addLast("SimpleTransferHandler", new SimpleTransferHandler(ctx.channel(), true));
				//log
				ch.pipeline().addLast("EventLoggerHandler",new EventLoggerHandler("ClientToProxyServer",true));

			}
		};
		ChannelFuture future = Connections.connect(ctx.channel().eventLoop(), getProxyAddress(), channelInitializer);
		Channel channel = future.channel();
		EventExecutor executor = ctx.executor();
		future.addListener((f)->{
			if (f.isSuccess()){
				Runnable task = ()->{
					//删除当前连接RequestToClient下ChannelHandler
					ctx.pipeline().forEach((entry) -> ctx.pipeline().remove(entry.getKey()));
					ctx.pipeline().addLast("ReadTimeoutHandler", new ReadTimeoutHandler(StaticConfig.timeout, TimeUnit.SECONDS));
					ctx.pipeline().addLast("ClientTransferHandler", new ClientTransferHandler(channel, false));
					ctx.pipeline().addLast("EventLoggerHandler", new EventLoggerHandler("RequestServer", true));


					channel.writeAndFlush(bindMessage);
				};
				if (executor.inEventLoop()){
					task.run();
				}else{
					executor.execute(task);
				}
			}else{
				logger.info(hostName + "connect failed");
                ctx.channel().close();
			}
		});
	}


	private SocketAddress getProxyAddress() throws Exception {
		InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(StaticConfig.PROXY_SERVER_ADDRESS), StaticConfig.PROXY_SERVER_PORT);
		return address;
	}
}
