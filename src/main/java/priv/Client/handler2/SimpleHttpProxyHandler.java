package priv.Client.handler2;

import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import priv.Client.bean.HostAndPort;
import priv.common.handler.EventLoggerHandler;
import priv.common.handler2.InboundCallBackHandler;
import priv.common.handler2.coder.AllMessageTransferHandler;
import priv.common.log.LogUtil;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 *  * @author  pyuan
 *  * @date    2019/8/29 0029
 *  * @Description
 *  *
 *  
 */
public class SimpleHttpProxyHandler extends ChannelDuplexHandler {
	public static final AttributeKey<HostAndPort> HOST_AND_PORT_ATTRIBUTE_KEY = AttributeKey.newInstance("HostAndPort");
	private Channel proxyChannel;
	private static final Logger logger = LoggerFactory.getLogger(SimpleHttpProxyHandler.class);
	public SimpleHttpProxyHandler() {
	}


	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof FullHttpRequest) {
			FullHttpRequest m = (FullHttpRequest) msg;
			if (HttpMethod.CONNECT.equals(m.method())) {
				ctx.fireChannelRead(msg);
				return;
			}
			try {
				final HostAndPort destination = HostAndPort.resolve(m);

				logger.debug(destination.toString());
				logger.debug(m.toString());

				handleSimpleProxy(ctx, m, destination);

			} catch (Throwable e) {
				logger.error(LogUtil.stackTraceToString(e));
				throw e;
			}
		}else{
			ctx.fireChannelRead(msg);
		}
	}

	private void handleSimpleProxy(ChannelHandlerContext ctx, FullHttpRequest msg, HostAndPort destination) throws Exception {
		ChannelFuture channelFuture;
		Channel thisChannel = ctx.channel();
		if (this.proxyChannel == null || (!this.proxyChannel.isActive())){
			InboundCallBackHandler callBack = new InboundCallBackHandler();
			callBack.setChannelReadListener(new BiConsumer<Channel, Object>() {
				@Override
				public void accept(Channel channel, Object o) {
					thisChannel.writeAndFlush(o);
				}
			});

			callBack.setChannelInactiveListener(new Consumer<Channel>() {
				@Override
				public void accept(Channel channel) {
					thisChannel.close();
				}
			});
			ChannelInitializer channelInitializer = new ChannelInitializer() {
				@Override
				protected void initChannel(Channel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
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


		boolean needToBind = !Objects.equals(destination,channelFuture.channel().attr(HOST_AND_PORT_ATTRIBUTE_KEY).get());
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

	private SocketAddress getProxyAddress() throws Exception {
		InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(StaticConfig.PROXY_SERVER_ADDRESS), StaticConfig.PROXY_SERVER_PORT);
		return address;
	}
}
