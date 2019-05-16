package Server;

import Server.handler.DestinationProxyHandler;
import common.handler.EventLoggerHandler;
import common.handler.ReadWriteTimeoutHandler;
import common.handler.coder.ByteBufToMessageInboundHandler;
import common.handler.coder.MessageToByteBufOutboundHandler;
import common.log.LogUtil;
import Server.handler.HeaderIdentifyHandler;
import common.util.HandlerHelper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import common.resource.SystemConfig;

import java.net.InetAddress;
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
        ProxyServer proxyServer = new ProxyServer(new InetSocketAddress(InetAddress.getLocalHost(),9002), new ChannelInitializer() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
//                pipeline.addLast("LengthFieldBasedFrameDecoder", HandlerHelper.newDefaultFrameDecoderInstance())
//                        .addLast("ReadWriteTimeoutHandler", new ReadWriteTimeoutHandler(SystemConfig.timeout))
//                        .addLast("ByteBufToMessageInboundHandler", new ByteBufToMessageInboundHandler())
//                        .addLast("MessageToByteBufOutboundHandler", new MessageToByteBufOutboundHandler())
//                        .addLast("HeaderIdentifyHandler", new HeaderIdentifyHandler())
//                        .addLast("DestinationProxyHandler", new DestinationProxyHandler())
//                        .addLast("EventLoggerHandler", new EventLoggerHandler((ctx, cause) -> "ProxyServer: "+EventLoggerHandler.DEFAULT_HANDLER.apply(ctx,cause)));
//            }
//        });
                pipeline.addLast("LengthFieldBasedFrameDecoder", HandlerHelper.newDefaultFrameDecoderInstance())
                        .addLast("ReadWriteTimeoutHandler", new ReadWriteTimeoutHandler(SystemConfig.timeout))
                        .addLast("ByteBufToMessageInboundHandler", new ByteBufToMessageInboundHandler())
                        .addLast("MessageToByteBufOutboundHandler", new MessageToByteBufOutboundHandler())
                        .addLast("HeaderIdentifyHandler", new HeaderIdentifyHandler())
                        .addLast("DestinationProxyHandler", new DestinationProxyHandler())
                        .addLast("EventLoggerHandler", new EventLoggerHandler((ctx, cause) -> "ProxyServer: "+EventLoggerHandler.DEFAULT_HANDLER.apply(ctx,cause)));
            }
        });
        proxyServer.serverChannel.closeFuture().addListener((f) -> {
            System.out.println("server stop");
            eventLoopGroup.shutdownGracefully();
        });
    }
}
