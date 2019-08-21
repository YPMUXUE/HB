package priv.Server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import priv.Server.handler2.DestinationProxyHandler;
import priv.common.handler.EventLoggerHandler;
import priv.common.handler.ReadWriteTimeoutHandler;
import priv.common.handler2.coder.AllMessageTransferHandler;
import priv.common.resource.StaticConfig;
import priv.common.util.HandlerHelper;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class ProxyServer {
    private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);
    static final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    protected Channel serverChannel;

    public ProxyServer(SocketAddress address, ChannelInitializer channelInitializer) throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(channelInitializer);
        ChannelFuture future = bootstrap.bind(address);
        future.addListener(f -> {
            if (f.isSuccess()) {
                logger.info( address.toString() + "bind success");
                Runtime.getRuntime().addShutdownHook(new Thread(() -> serverChannel.close().syncUninterruptibly()));
            }
        });
        future.syncUninterruptibly();
        this.serverChannel = future.channel();
    }


    public static void main(String[] args) throws Exception {
        ProxyServer proxyServer = new ProxyServer(new InetSocketAddress(StaticConfig.PROXY_SERVER_ADDRESS,StaticConfig.PROXY_SERVER_PORT), new ChannelInitializer() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                pipeline.addLast("LengthFieldBasedFrameDecoder", HandlerHelper.newDefaultFrameDecoderInstance())
                        .addLast("ReadWriteTimeoutHandler", new ReadWriteTimeoutHandler(120))
                        .addLast("AllMessageTransferHandler", new AllMessageTransferHandler())
                        .addLast("DestinationProxyHandler", new DestinationProxyHandler())
                        .addLast("EventLoggerHandler", new EventLoggerHandler("ProxyServer", true));
            }
        });
        proxyServer.serverChannel.closeFuture().addListener((f) -> {
            System.out.println("server stop");
            eventLoopGroup.shutdownGracefully();
        });
    }
}
