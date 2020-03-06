package priv.Client.handler2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import priv.Client.bean.HostAndPort;
import priv.common.handler.EventLoggerHandler;
import priv.common.handler2.AesEcbCryptHandler;
import priv.common.handler2.ConnectProxyHandler;
import priv.common.handler2.InboundCallBackHandler;
import priv.common.handler2.coder.AllMessageTransferHandler;
import priv.common.message.ChannelDataEntry;
import priv.common.message.frame.Message;
import priv.common.message.frame.bind.BindV2Message;
import priv.common.message.frame.close.CloseMessage;
import priv.common.message.frame.connect.ConnectMessage;
import priv.common.message.frame.establish.ConnectionEstablishFailedMessage;
import priv.common.message.frame.establish.ConnectionEstablishMessage;
import priv.common.resource.ConnectionEvents;
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

public class HttpMethodHandler extends ChannelDuplexHandler {
	private static final Logger logger = LoggerFactory.getLogger(HttpMethodHandler.class);
	private static final String callBackHandlerName = "InboundCallBackHandler";
	public static final AttributeKey<HostAndPort> HOST_AND_PORT_ATTRIBUTE_KEY = AttributeKey.newInstance("HostAndPort");
	private ChannelFuture bindPromise;
	private boolean isConnect = false;
	private EmbeddedChannel embeddedChannel;
	public HttpMethodHandler() {
		this.embeddedChannel = new EmbeddedChannel(new HttpServerCodec(),new HttpObjectAggregator(0x7FFFFFFF));
	}


	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("isConnect:{},msg:{}", this.isConnect, ByteBufUtil.hexDump((ByteBuf) msg));
		}

		if (this.isConnect){
			sendConnectMessage(ctx,(ByteBuf)msg);
			return;
		}

		boolean hasResult = this.embeddedChannel.writeInbound(msg);
		if (!hasResult) {
			return;
		}

		FullHttpRequest m = this.embeddedChannel.readInbound();
		try {
			if (!m.decoderResult().isSuccess()) {
				logger.error("http decode failure", m.decoderResult().cause());
				ctx.writeAndFlush(Unpooled.buffer().writeBytes(HttpResources.HttpResponse.HTTP_400.getBytes(StandardCharsets.UTF_8)));
				return;
			}

			final HostAndPort destination = HostAndPort.resolve(m);
			if (logger.isDebugEnabled()) {
				logger.debug(destination.toString());
			}
			if (HttpMethod.CONNECT.equals(m.method())) {
//				handleConnectRequest(ctx, m, destination);
				throw new Exception("");
			} else {
				commonHttpRequest(ctx, m.retain(), destination);
			}
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}

	private void sendConnectMessage(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
throw new Exception();
	}

	private void commonHttpRequest(ChannelHandlerContext ctx, FullHttpRequest msg, HostAndPort destination) throws Exception {
		Channel proxyChannel = Objects.isNull(this.bindPromise) ? null : this.bindPromise.channel();
		Channel thisChannel = ctx.channel();
		ChannelFuture bindFuture;
		if (proxyChannel != null && proxyChannel.isActive()){
			HostAndPort oldDes = proxyChannel.attr(HOST_AND_PORT_ATTRIBUTE_KEY).get();
			if (Objects.equals(destination,oldDes)){
				bindFuture = this.bindPromise;
			}else{
				Channel oldChannel = this.bindPromise.channel();
				oldChannel.eventLoop().execute(() -> oldChannel.close());
				bindFuture = bindRemote(ctx,destination);
				this.bindPromise = bindFuture;
			}
		}else{
			bindFuture = bindRemote(ctx,destination);
			this.bindPromise = bindFuture;
		}

		msg.headers().remove("Proxy-Connection");

		this.bindPromise.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()){
					future.channel().writeAndFlush(msg).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
				}else{
					ReferenceCountUtil.release(msg);
				}
			}
		});

	}

	private ChannelFuture bindRemote(ChannelHandlerContext ctx, HostAndPort destination) throws Exception {
		Channel thisChannel = ctx.channel();
		ChannelPromise bindPromise = thisChannel.newPromise();
		InboundCallBackHandler callBack = new InboundCallBackHandler();
		callBack.setChannelReadListener(new BiConsumer<ChannelHandlerContext, Object>() {
			@Override
			public void accept(ChannelHandlerContext c, Object o) {
				if (o instanceof Message) {
					ConnectionEvents events =((Message) o).supportConnectionEvent();
					switch (events){
						case CONNECTION_ESTABLISH:
							c.channel().attr(HOST_AND_PORT_ATTRIBUTE_KEY).set(destination);
							bindPromise.setSuccess();
							break;
						case CONNECTION_ESTABLISH_FAILED:
							bindPromise.setFailure(new Exception("Establish Failed"));
							break;
						case CLOSE:
							thisChannel.write(new ChannelDataEntry(c.channel(), (CloseMessage) o));
							break;
						case CONNECT:
							thisChannel.write(new ChannelDataEntry(c.channel(), (ConnectMessage) o));
						default:
							ReferenceCountUtil.release(o);
							throw new IllegalArgumentException("unknown message:" + o.toString());
					}
				}else{
					throw new IllegalArgumentException("unknow object:" + o.toString());
				}
			}
		});

		callBack.setChannelInactiveListener(new Consumer<ChannelHandlerContext>() {
			@Override
			public void accept(ChannelHandlerContext c) {
				thisChannel.write(new ChannelDataEntry(c.channel(),new CloseMessage()));
			}
		});

		ChannelInitializer<Channel> channelInitializer = new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				byte[] aesKey = Base64.decodeBase64(StaticConfig.AES_KEY);
				ChannelPipeline pipeline = ch.pipeline();
				pipeline.addLast("AESHandler",new AesEcbCryptHandler(aesKey));
				pipeline.addLast(HandlerHelper.newDefaultFrameDecoderInstance());
				pipeline.addLast(new AllMessageTransferHandler());
//				pipeline.addLast(new HttpProxyMessageHandler());
				pipeline.addLast(callBack);
				pipeline.addLast(new EventLoggerHandler("Proxy Channel"));
			}
		};

		ChannelFuture connectFuture = Connections.connect(ctx.channel().eventLoop(),getProxyAddress(),channelInitializer);

		final BindV2Message bindMessage = new BindV2Message(destination.getHostString(),destination.getPort());
		connectFuture.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()){
					future.channel().writeAndFlush(bindMessage);
				}else{
					bindPromise.tryFailure(future.cause());
				}
			}
		});
		return bindPromise;
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

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (msg instanceof ByteBuf){
			ctx.write(msg,promise);
		}else if (msg instanceof ChannelDataEntry){
			Channel proxyChannel = ((ChannelDataEntry) msg).getBindChannel();
			Channel thisBindChannel = this.bindPromise.channel();
			if (!Objects.equals(proxyChannel, thisBindChannel)){
				thisBindChannel.eventLoop().execute(new Runnable() {
					@Override
					public void run() {
						thisBindChannel.close();
					}
				});
				return;
			}
			Object data = ((ChannelDataEntry) msg).getData();
			if (data instanceof Message){
				ConnectionEvents event = ((Message) data).supportConnectionEvent();
				switch (event){
					case CLOSE:
						ctx.channel().close();
						ReferenceCountUtil.release(data);
						break;
					case CONNECT:
						assert data instanceof ConnectMessage;
						ByteBuf content = ((ConnectMessage) data).getContent();
						ctx.write(content);
						break;
					default:
						throw new IllegalArgumentException("unknown Message :"+data.toString());
				}
			}else{
				throw new IllegalArgumentException("unknown object:"+data.toString());
			}
		}else{
			throw new IllegalArgumentException("unknown object:"+msg.toString());
		}
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
		private static final int CONNECTION_SERVICE_UNAVAILABLE = 3;
		private final int type;
		private final Channel channel;

		public ConnectionEvent(int type, Channel channel) {
			this.type = type;
			this.channel = channel;
		}
	}
}
