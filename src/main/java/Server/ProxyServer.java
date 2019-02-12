package Server;

import common.handler.EventLoggerHandler;
import common.handler.ReadWriteTimeoutHandler;
import common.handler.coder.ByteBufToMessageInboundHandler;
import common.handler.coder.MessageToByteBufOutboundHandler;
import common.log.LogUtil;
import Server.handler.DestinationConnectHandler;
import Server.handler.HeaderIdentifyHandler;
import common.util.HandlerHelper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import common.resource.SystemConfig;

import java.net.InetAddress;

public class ProxyServer {
    static final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    protected Channel serverChannel;

    public ProxyServer(int port, ChannelInitializer channelInitializer) throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(channelInitializer);
        InetAddress localAddr = InetAddress.getLocalHost();
        ChannelFuture future = bootstrap.bind(localAddr, port);
        future.addListener(f -> {
            if (f.isSuccess()) {
                LogUtil.info(() -> localAddr.toString() + "success bind on port " + port);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> serverChannel.close().syncUninterruptibly()));
            }
        });
        future.syncUninterruptibly();
        this.serverChannel = future.channel();
    }


    public static void main(String[] args) throws Exception {
        ProxyServer proxyServer = new ProxyServer(9002, new ChannelInitializer() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
//                pipeline.addLast("LengthFieldBasedFrameDecoder", HandlerHelper.newDefaultFrameDecoderInstance())
//                        .addLast("ReadWriteTimeoutHandler",new ReadWriteTimeoutHandler(SystemConfig.timeout))
//                        .addLast("HeaderIdentifyHandler",new HeaderIdentifyHandler())
//                        .addLast("DestinationConnectHandler",new DestinationConnectHandler())
//                .addLast("EventLoggerHandler",new EventLoggerHandler((ctx, cause)->"EventLoggerHandler:ProxyServer "+ctx.channel().toString()+":"+cause.toString()));
//            }
//        });
                pipeline.addLast("LengthFieldBasedFrameDecoder", HandlerHelper.newDefaultFrameDecoderInstance())
                        .addLast("ReadWriteTimeoutHandler", new ReadWriteTimeoutHandler(SystemConfig.timeout))
                        .addLast("ByteBufToMessageInboundHandler", new ByteBufToMessageInboundHandler())
                        .addLast("MessageToByteBufOutboundHandler", new MessageToByteBufOutboundHandler())
                        .addLast("HeaderIdentifyHandler", new HeaderIdentifyHandler())
                        .addLast("DestinationConnectHandler", new DestinationConnectHandler())
                        .addLast("EventLoggerHandler", new EventLoggerHandler((ctx, cause) -> "EventLoggerHandler:ProxyServer " + ctx.channel().toString() + ":" + cause.toString()));
            }
        });
        proxyServer.serverChannel.closeFuture().addListener((f) -> {
            System.out.println("server stop");
            eventLoopGroup.shutdownGracefully();
        });

    }
}
