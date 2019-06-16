package Server;

import Server.handler2.DestinationProxyHandler;
import common.handler.EventLoggerHandler;
import common.handler.ReadWriteTimeoutHandler;
import common.handler2.coder.AllMessageTransferHandler;
import common.log.LogUtil;
import common.util.HandlerHelper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class ProxyServer {
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
                LogUtil.info(() -> address.toString() + "bind success");
                Runtime.getRuntime().addShutdownHook(new Thread(() -> serverChannel.close().syncUninterruptibly()));
            }
        });
        future.syncUninterruptibly();
        this.serverChannel = future.channel();
    }


    public static void main(String[] args) throws Exception {
        ProxyServer proxyServer = new ProxyServer(new InetSocketAddress("127.0.0.1",9002), new ChannelInitializer() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                pipeline.addLast("LengthFieldBasedFrameDecoder", HandlerHelper.newDefaultFrameDecoderInstance())
                        .addLast("ReadWriteTimeoutHandler", new ReadWriteTimeoutHandler(120))
                        .addLast("ByteBufToMessageInboundHandler", new AllMessageTransferHandler())
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
