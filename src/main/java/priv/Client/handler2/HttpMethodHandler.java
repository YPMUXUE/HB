package priv.Client.handler2;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import priv.Client.bean.HostAndPort;
import priv.common.events.ConnectFailedProxyEvent;
import priv.common.handler.EventLoggerHandler;
import priv.common.handler2.AesEcbCryptHandler;
import priv.common.handler2.ConnectProxyHandler;
import priv.common.handler2.InboundCallBackHandler;
import priv.common.handler2.coder.AllMessageTransferHandler;
import priv.common.log.LogUtil;
import priv.common.message.frame.bind.BindV2Message;
import priv.common.message.frame.close.CloseMessage;
import priv.common.message.frame.connect.ConnectMessage;
import priv.common.message.frame.establish.ConnectionEstablishFailedMessage;
import priv.common.message.frame.establish.ConnectionEstablishMessage;
import priv.common.resource.HttpResources;
import priv.common.resource.StaticConfig;
import priv.common.util.Connections;
import priv.common.util.HandlerHelper;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HttpMethodHandler extends ChannelInboundHandlerAdapter {
	private static final String callBackHandlerName = "InboundCallBackHandler";
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
				if (logger.isDebugEnabled()) {
					logger.debug(destination.toString());
					logger.debug(m.toString());
				}
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
					pipeline.addLast(new EventLoggerHandler("HTTP Request Proxy Channel"));
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
				}else{
					ReferenceCountUtil.release(msg);
				}
			}
		});

	}

	private void handleConnectRequest(ChannelHandlerContext ctx, FullHttpRequest msg, HostAndPort destination) throws Exception {
		final Channel clientChannel = ctx.channel();
		EventLoop clientEventExecutors = clientChannel.eventLoop();
		BindV2Message bindMessage = new BindV2Message(destination.getHostString(), destination.getPort());
//		checkValid(bindMessage);
		InboundCallBackHandler callBackHandler = new InboundCallBackHandler();
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

				ch.pipeline().addLast(callBackHandlerName, callBackHandler);
				//log
				ch.pipeline().addLast("EventLoggerHandler", new EventLoggerHandler("ClientToProxyServer"));
			}
		};
		callBackHandler.setChannelReadListener(new BiConsumer<ChannelHandlerContext, Object>() {
			@Override
			public void accept(ChannelHandlerContext c, Object o) {
				if (o instanceof ConnectionEstablishMessage){
					clientChannel.pipeline().fireUserEventTriggered(new ConnectionEvent(ConnectionEvent.CONNECTION_ESTABLISHED,c.channel()));
				}else if (o instanceof ConnectionEstablishFailedMessage){
					logger.info(((ConnectionEstablishFailedMessage) o).getReason());
					clientChannel.pipeline().fireUserEventTriggered(new ConnectionEvent(ConnectionEvent.CONNECTION_ESTABLISH_FAILED,c.channel()));
				}else{
					ReferenceCountUtil.release(o);
					throw new IllegalArgumentException("unexpected class type:"+o.getClass().toString());
				}
			}
		});

		ChannelFuture connectPromise = Connections.connect(clientEventExecutors, getProxyAddress(), channelInitializer);
		connectPromise.addListener((ChannelFutureListener) (f) -> {
			if (f.isSuccess()){
				f.channel().writeAndFlush(bindMessage);
				}else{
				logger.error("connect error:",f.cause());
				clientChannel.pipeline().fireUserEventTriggered(new ConnectionEvent(ConnectionEvent.CONNECTION_ESTABLISH_FAILED,f.channel()));
			}
		});
	}

	private void checkValid(BindV2Message bindMessage) {

	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (!(evt instanceof ConnectionEvent)){
			super.userEventTriggered(ctx, evt);
			return;
		}
		ConnectionEvent e = (ConnectionEvent) evt;
		if (e.type == ConnectionEvent.CONNECTION_ESTABLISHED){
			Channel targetChannel = e.channel;
			changeToConnect(ctx, targetChannel);
		}else if (e.type == ConnectionEvent.CONNECTION_ESTABLISH_FAILED){
			byte[] response = HttpResources.HttpResponse.Connection_Failed.getBytes(StandardCharsets.UTF_8);
			ctx.writeAndFlush(Unpooled.buffer(response.length).writeBytes(response));
		}else {
			throw new IllegalArgumentException("unexpect type of ConnectionEvent:"+e.type);
		}
	}

	private void changeToConnect(ChannelHandlerContext ctx, Channel targetChannel) {
		InboundCallBackHandler callBackHandler = new InboundCallBackHandler();
		targetChannel.pipeline().replace(callBackHandlerName,callBackHandlerName,callBackHandler);
		callBackHandler.setChannelReadListener(new BiConsumer<ChannelHandlerContext, Object>() {
			@Override
			public void accept(ChannelHandlerContext channelHandlerContext, Object o) {
				ctx.channel().writeAndFlush(o);
			}
		});
		callBackHandler.setChannelInactiveListener(new Consumer<ChannelHandlerContext>() {
			@Override
			public void accept(ChannelHandlerContext channelHandlerContext) {
				ctx.channel().write(new CloseMessage());
			}
		});
				//删除当前连接下ChannelHandler
		ctx.pipeline().forEach((entry) -> ctx.pipeline().remove(entry.getKey()));
//		ctx.pipeline().addLast("ReadTimeoutHandler", new ReadTimeoutHandler(StaticConfig.timeout, TimeUnit.SECONDS));
		ctx.pipeline().addLast("ConnectProxyHandler", new ConnectProxyHandler());
		ctx.pipeline().addLast("EventLoggerHandler", new EventLoggerHandler("RequestServer"));

	}

	private SocketAddress getProxyAddress() throws Exception {
		InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(StaticConfig.PROXY_SERVER_ADDRESS), StaticConfig.PROXY_SERVER_PORT);
		return address;
	}
	private static class ConnectionEvent{
		private static final int CONNECTION_ESTABLISHED = 1;
		private static final int CONNECTION_ESTABLISH_FAILED = 2;
		private final int type;
		private final Channel channel;

		public ConnectionEvent(int type, Channel channel) {
			this.type = type;
			this.channel = channel;
		}
	}
}
