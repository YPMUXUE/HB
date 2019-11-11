package priv.Client.handler2;

import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import priv.Client.bean.HostAndPort;
import priv.common.events.ConnectFailedProxyEvent;
import priv.common.events.ConnectSuccessProxyEvent;
import priv.common.handler.EventLoggerHandler;
import priv.common.handler2.AesEcbCryptHandler;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HttpMethodHandler extends ChannelInboundHandlerAdapter {
	public static final AttributeKey<HostAndPort> HOST_AND_PORT_ATTRIBUTE_KEY = AttributeKey.newInstance("HostAndPort");
	private Channel proxyChannel;
	private static final Logger logger = LoggerFactory.getLogger(HttpMethodHandler.class);
	public HttpMethodHandler() {
	}


	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		try {
			if (msg instanceof FullHttpRequest) {
				FullHttpRequest m = (FullHttpRequest) msg;
				final HostAndPort destination = HostAndPort.resolve(m);

				logger.debug(destination.toString());
				logger.debug(m.toString());
				if (HttpMethod.CONNECT.equals(m.method())) {
					handleConnectRequest(ctx, m, destination);
				}else{
					handleSimpleHttpRequest(ctx, m.retain(), destination);
				}
			}
		} catch (Throwable e) {
			logger.error(LogUtil.stackTraceToString(e));
		} finally {
			ReferenceCountUtil.release(msg);
		}

	}

	private void handleSimpleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest msg, HostAndPort destination) throws Exception {
		ChannelFuture channelFuture;
		Channel thisChannel = ctx.channel();
		boolean isReconnect = false;
		if (this.proxyChannel == null || (!this.proxyChannel.isActive())){
			InboundCallBackHandler callBack = new InboundCallBackHandler();
			callBack.setChannelReadListener(new BiConsumer<ChannelHandlerContext, Object>() {
				@Override
				public void accept(ChannelHandlerContext c, Object o) {
					thisChannel.writeAndFlush(o);
				}
			});

			callBack.setChannelInactiveListener(new Consumer<ChannelHandlerContext>() {
				@Override
				public void accept(ChannelHandlerContext c) {
					thisChannel.close();
				}
			});
			ChannelInitializer channelInitializer = new ChannelInitializer() {
				@Override
				protected void initChannel(Channel ch) throws Exception {
					byte[] aesKey = Base64.decodeBase64(StaticConfig.AES_KEY);
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast("AESHandler",new AesEcbCryptHandler(aesKey));
					pipeline.addLast(HandlerHelper.newDefaultFrameDecoderInstance());
					pipeline.addLast(new AllMessageTransferHandler());
					pipeline.addLast(new HttpProxyMessageHandler());
					pipeline.addLast(new HttpRequestEncoder());
					pipeline.addLast(callBack);
					pipeline.addLast(new EventLoggerHandler("HTTP Request Proxy Channel",true));
				}
			};
			channelFuture = Connections.connect(ctx.channel().eventLoop(), getProxyAddress(), channelInitializer);

			this.proxyChannel = channelFuture.channel();
			proxyChannel.attr(HOST_AND_PORT_ATTRIBUTE_KEY).set(destination);
			isReconnect = true;
		}else{
			channelFuture = proxyChannel.newSucceededFuture();
		}
		BindV2Message bindMessage = new BindV2Message(destination.getHostString(),destination.getPort());

		List<Map.Entry<String, String>> entries = msg.headers().entries();
		List<String> proxyHeaders = new ArrayList<>();
		for (Map.Entry<String, String> s : entries) {
			if (s.getKey().startsWith("Proxy")) {
				proxyHeaders.add(s.getKey());
			}
		}
		proxyHeaders.forEach((s)->{
			msg.headers().remove(s);
		});

		boolean isNewAddr = !Objects.equals(destination,channelFuture.channel().attr(HOST_AND_PORT_ATTRIBUTE_KEY).get());
		boolean needToBind = isNewAddr || isReconnect;
		channelFuture.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				Channel channel = future.channel();
				if (future.isSuccess()){
					if (needToBind) {
						channel.write(bindMessage);
					}
					channel.writeAndFlush(msg);
				}
			}
		});

	}

	private void handleConnectRequest(ChannelHandlerContext ctx, FullHttpRequest msg, HostAndPort destination) throws Exception {
		final Channel clientChannel = ctx.channel();
		Message bindMessage = new BindV2Message(destination.getHostString(), destination.getPort());
		InboundCallBackHandler callBackHandler = new InboundCallBackHandler();
		callBackHandler.setChannelReadListener(new BiConsumer<ChannelHandlerContext, Object>() {
			@Override
			public void accept(ChannelHandlerContext c, Object o) {
				clientChannel.writeAndFlush(o);
			}
		});

		callBackHandler.setExceptionCaughtListener(new BiConsumer<ChannelHandlerContext, Throwable>() {
			@Override
			public void accept(ChannelHandlerContext c, Throwable throwable) {
				logger.error(c.channel().toString(), throwable);
				c.channel().close();
			}
		});

		callBackHandler.setChannelInactiveListener(new Consumer<ChannelHandlerContext>() {
			@Override
			public void accept(ChannelHandlerContext c) {
				clientChannel.close();
			}
		});

		callBackHandler.setChannelActiveListener(new Consumer<ChannelHandlerContext>() {
			@Override
			public void accept(ChannelHandlerContext c) {
				clientChannel.pipeline().fireUserEventTriggered(new ConnectSuccessProxyEvent(c.channel()));
				c.channel().writeAndFlush(bindMessage);
				clientChannel.closeFuture().addListener(f -> c.channel().eventLoop().execute(()->{c.channel().close();}));
			}
		});

		ChannelInitializer channelInitializer = new ChannelInitializer() {
			@Override
			protected void initChannel(Channel ch) throws Exception {

				byte[] aesKey = Base64.decodeBase64(StaticConfig.AES_KEY);
				ch.pipeline().addLast("AESHandler",new AesEcbCryptHandler(aesKey));
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
