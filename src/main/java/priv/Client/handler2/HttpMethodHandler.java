package priv.Client.handler2;

import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import priv.Client.bean.HostAndPort;
import priv.common.events.ConnectFailedProxyEvent;
import priv.common.events.ConnectSuccessProxyEvent;
import priv.common.handler.EventLoggerHandler;
import priv.common.handler2.ConnectProxyHandler;
import priv.common.handler2.InboundCallBackHandler;
import priv.common.handler2.coder.AllMessageTransferHandler;
import priv.common.log.LogUtil;
import priv.common.message.frame.Message;
import priv.common.message.frame.bind.BindV2Message;
import priv.common.resource.StaticConfig;
import priv.common.util.Connections;
import priv.common.util.HandlerHelper;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HttpMethodHandler extends ChannelInboundHandlerAdapter {
	private Channel proxyChannel;
	private static final Logger logger = LoggerFactory.getLogger(HttpMethodHandler.class);
	public HttpMethodHandler() {
	}


	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		try {
			if (msg instanceof FullHttpRequest) {
				FullHttpRequest m = (FullHttpRequest) msg;
				final HostAndPort destination = HostAndPort.resolve(m);
				if (this.proxyChannel != null) {
					//原有连接
					HostAndPort hostAndPort = this.proxyChannel.attr(ConfigAttributeKey.HOST_AND_PORT_ATTRIBUTE_KEY).get();
					if (!Objects.equals(destination,hostAndPort)){
						Channel thisChannel = this.proxyChannel;
						this.proxyChannel = null;
						thisChannel.close();
					}
				}

				logger.debug(destination.toString());
				logger.debug(m.toString());
				if (HttpMethod.CONNECT.equals(m.method())) {
					handleConnect(ctx, m, destination);
				}
			}
		} catch (Throwable e) {
			logger.error(LogUtil.stackTraceToString(e));
		} finally {
			ReferenceCountUtil.release(msg);
		}

	}

	private void handleSimpleProxy(ChannelHandlerContext ctx, FullHttpRequest msg, HostAndPort destination) {
		throw new UnsupportedOperationException("普通HTTP代理请求还没完成，先放着");
	}

	private void handleConnect(ChannelHandlerContext ctx, FullHttpRequest msg, HostAndPort destination) throws Exception {
		final Channel clientChannel = ctx.channel();
		Message bindMessage = new BindV2Message(destination.getHostString(), destination.getPort());
		InboundCallBackHandler callBackHandler = new InboundCallBackHandler();
		callBackHandler.setChannelReadListener(new BiConsumer<Channel, Object>() {
			@Override
			public void accept(Channel c, Object o) {
				clientChannel.writeAndFlush(o);
			}
		});

		callBackHandler.setExceptionCaughtListener(new BiConsumer<Channel, Throwable>() {
			@Override
			public void accept(Channel targetChannel, Throwable throwable) {
				logger.error(targetChannel.toString(), throwable);
				targetChannel.close();
			}
		});

		callBackHandler.setChannelInactiveListener(new Consumer<Channel>() {
			@Override
			public void accept(Channel channel) {
				clientChannel.close();
			}
		});

		callBackHandler.setChannelActiveListener(new Consumer<Channel>() {
			@Override
			public void accept(Channel channel) {
				clientChannel.pipeline().fireUserEventTriggered(new ConnectSuccessProxyEvent(channel));
				channel.writeAndFlush(bindMessage);
				clientChannel.closeFuture().addListener(f -> channel.eventLoop().execute(()->{channel.close();}));
			}
		});

		ChannelInitializer channelInitializer = new ChannelInitializer() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				//inbound
				ch.pipeline().addLast("LengthFieldBasedFrameDecoder", HandlerHelper.newDefaultFrameDecoderInstance());
				//消息转换
				ch.pipeline().addLast("MessageTransferHandler", new AllMessageTransferHandler());
				//inbound消息转发
				ch.pipeline().addLast("InboundCallBackHandler", callBackHandler);
				//log
				ch.pipeline().addLast("EventLoggerHandler", new EventLoggerHandler("ClientToProxyServer", true));
			}
		};



		//删除当前连接下ChannelHandler
		ctx.pipeline().forEach((entry) -> ctx.pipeline().remove(entry.getKey()));
		ctx.pipeline().addLast("ReadTimeoutHandler", new ReadTimeoutHandler(StaticConfig.timeout, TimeUnit.SECONDS));
		ctx.pipeline().addLast("ConnectProxyHandler", new ConnectProxyHandler());
		ctx.pipeline().addLast("EventLoggerHandler", new EventLoggerHandler("RequestServer", true));


		ChannelFuture connectPromise = Connections.connect(clientChannel.eventLoop(), getProxyAddress(), channelInitializer);
		connectPromise.addListener((ChannelFutureListener) (f) -> {
			if (!f.isSuccess()){
				clientChannel.pipeline().fireUserEventTriggered(new ConnectFailedProxyEvent(f.cause()));
			}
		});
	}


	private SocketAddress getProxyAddress() throws Exception {
		InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(StaticConfig.PROXY_SERVER_ADDRESS), StaticConfig.PROXY_SERVER_PORT);
		return address;
	}
}
