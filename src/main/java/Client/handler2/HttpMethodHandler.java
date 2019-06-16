package Client.handler2;

import Client.bean.HostAndPort;
import common.handler.EventLoggerHandler;
import common.handler.SimpleTransferHandler;
import common.handler2.coder.AllMessageTransferHandler;
import common.log.LogUtil;
import common.message.frame.Message;
import common.message.frame.bind.BindV2Message;
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

	public HttpMethodHandler() {
	}


	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		try {
			if (msg instanceof FullHttpRequest) {
				FullHttpRequest m = (FullHttpRequest) msg;
				LogUtil.debug(m::toString);

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
			LogUtil.info(() -> LogUtil.stackTraceToString(e));
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
				ch.pipeline().addLast("test",new ChannelDuplexHandler(){
					@Override
					public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
						System.out.println("123123123123");
						super.close(ctx, promise);
					}

					@Override
					public void channelInactive(ChannelHandlerContext ctx) throws Exception {
						System.out.println("inactive");
						super.channelInactive(ctx);
					}
				});
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
					ctx.pipeline().addLast("ReadTimeoutHandler", new ReadTimeoutHandler(SystemConfig.timeout, TimeUnit.SECONDS));
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
				LogUtil.info(() -> (hostName + "connect failed"));
                ctx.channel().close();
			}
		});
	}


	private SocketAddress getProxyAddress() throws Exception {
		InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(StaticConfig.PROXY_SERVER_ADDRESS), StaticConfig.PROXY_SERVER_PORT);
		return address;
	}
}
