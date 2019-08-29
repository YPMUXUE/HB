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
		if (this.proxyChannel == null || (!this.proxyChannel.isActive())){
			ChannelInitializer channelInitializer = new ChannelInitializer() {
				@Override
				protected void initChannel(Channel ch) throws Exception {

				}
			};
			ChannelFuture channelFuture = Connections.connect(ctx.channel().eventLoop(), getProxyAddress(), channelInitializer);
		}

	}

	private SocketAddress getProxyAddress() throws Exception {
		InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(StaticConfig.PROXY_SERVER_ADDRESS), StaticConfig.PROXY_SERVER_PORT);
		return address;
	}
}
