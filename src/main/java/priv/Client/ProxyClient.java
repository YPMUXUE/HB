package priv.Client;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import priv.Client.handler2.HttpMethodHandler;
import priv.common.handler.EventLoggerHandler;
import priv.common.handler.ReadWriteTimeoutHandler;
import priv.common.handler2.coder.MessageOutboundTransferHandler;
import priv.common.log.LogUtil;
import priv.common.message.MessageTranslatorFactory;
import priv.common.resource.StaticConfig;
import priv.common.resource.SystemConfig;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

public class ProxyClient {
	public static final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
	private static final Logger logger = LoggerFactory.getLogger(ProxyClient.class);
	protected Channel serverChannel;

	public ProxyClient(final SocketAddress address, ChannelInitializer channelInitializer) throws Exception {
		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(eventLoopGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(channelInitializer);
		ChannelFuture future = bootstrap.bind(address);
		this.serverChannel = future.channel();
		future.addListener(f->{
			if (f.isSuccess()){
				logger.info("start success on" + address);
				Runtime.getRuntime().addShutdownHook(new Thread(() -> {
					serverChannel.close().syncUninterruptibly();
				}));
			}else{
				logger.error(LogUtil.stackTraceToString(future.cause()));
				serverChannel.close().syncUninterruptibly();
			}
		});
		future.syncUninterruptibly();
	}

	public static void main(String[] args) throws Exception {
		InetSocketAddress address=new InetSocketAddress(InetAddress.getByName(StaticConfig.LOCAL_HOST_ADDRESS),StaticConfig.LOCAL_HOST_PORT);
		ProxyClient proxyClient=new ProxyClient(address, new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new ChannelInboundHandlerAdapter(){
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    	if (logger.isDebugEnabled()) {
		                    logger.debug("{}:{}",ctx.channel(), ByteBufUtil.hexDump(((ByteBuf) msg).retain()));
	                    }
                        super.channelRead(ctx, msg);
                    }
                });
				pipeline.addLast("HttpRequestDecoder",new HttpRequestDecoder());
				//TODO 由于message包长度字段目前设置为4 所以消息长度大于等于4会有问题
				pipeline.addLast("HttpObjectAggregator",new HttpObjectAggregator(0x7FFFFFFF));
				pipeline.addLast("ReadWriteTimeoutHandler",new ReadWriteTimeoutHandler(StaticConfig.timeout));
//                pipeline.addLast("SimpleHttpProxyHandler",new SimpleHttpProxyHandler());
				pipeline.addLast("HttpMethodHandler",new HttpMethodHandler());
				pipeline.addLast(new MessageOutboundTransferHandler(MessageTranslatorFactory.ALL_TRANSLATORS));
				pipeline.addLast("ExceptionHandler",new EventLoggerHandler("ProxyClient"));
			}
		});
		proxyClient.serverChannel.closeFuture().addListener((f) -> {
			System.out.println("server stop");
			eventLoopGroup.shutdownGracefully();
		});
	}

}
