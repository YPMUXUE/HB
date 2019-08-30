package priv.Client.handler2;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.util.concurrent.SucceededFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import priv.Client.bean.HostAndPort;
import priv.common.handler2.coder.AllMessageTransferHandler;
import priv.common.log.LogUtil;
import priv.common.message.frame.bind.BindV2Message;
import priv.common.message.frame.connect.ConnectMessage;
import priv.common.resource.StaticConfig;
import priv.common.util.Connections;
import priv.common.util.HandlerHelper;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 *  * @author  pyuan
 *  * @date    2019/8/29 0029
 *  * @Description
 *  *
 *  
 */
public class SimpleHttpProxyHandler extends ChannelDuplexHandler {
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
//				if (this.proxyChannel != null) {
//					//原有连接
//					HostAndPort hostAndPort = this.proxyChannel.attr(ConfigAttributeKey.HOST_AND_PORT_ATTRIBUTE_KEY).get();
//					if (!Objects.equals(destination, hostAndPort)) {
//						Channel thisChannel = this.proxyChannel;
//						this.proxyChannel = null;
//						thisChannel.close();
//					}
//				}

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
		if (this.proxyChannel == null || (!this.proxyChannel.isActive())){
			ChannelInitializer channelInitializer = new ChannelInitializer() {
				@Override
				protected void initChannel(Channel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast(HandlerHelper.newDefaultFrameDecoderInstance());
					pipeline.addLast(new AllMessageTransferHandler());
					pipeline.addLast(new HttpProxyMessageHandler());
					pipeline.addLast(new HttpResponseDecoder());
					pipeline.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
					pipeline.addLast(new HttpRequestEncoder());
					pipeline.addLast(new ChannelInboundHandlerAdapter(){
						@Override
						public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
							ctx.channel().writeAndFlush(msg);
						}
					});
				}
			};
			channelFuture = Connections.connect(ctx.channel().eventLoop(), getProxyAddress(), channelInitializer);

			this.proxyChannel = channelFuture.channel();
			proxyChannel.attr(ConfigAttributeKey.HOST_AND_PORT_ATTRIBUTE_KEY).set(destination);
		}else{
			channelFuture = proxyChannel.newSucceededFuture();
		}
		BindV2Message bindMessage = new BindV2Message(destination.getHostString(),destination.getPort());
//		msg.headers().remove()
		channelFuture.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				Channel channel = future.channel();
				if (future.isSuccess()){
					channel.writeAndFlush(bindMessage);
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
