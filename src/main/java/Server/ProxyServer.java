package Server;

import Client.log.LogUtil;
import Server.handler.DestinationConnectHandler;
import Server.handler.ExceptionLoggerHandler;
import Server.handler.HeaderIdentifyHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.net.InetAddress;

public class ProxyServer {
    public static final int COUNT_OF_PROCESSORS = Runtime.getRuntime().availableProcessors();
    public static final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    public ProxyServer(int port,ChannelInitializer channelInitializer) throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(channelInitializer);
        InetAddress localAddr=InetAddress.getLocalHost();
        ChannelFuture future = bootstrap.bind(localAddr, port);
        future.addListener(f->{
            if (f.isSuccess()){
                LogUtil.info(()->localAddr.toString()+" bind success");
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    serverChannel.close().syncUninterruptibly();
                }));
            }
        });
        future.syncUninterruptibly();
        this.serverChannel = future.channel();
    }

    protected Channel serverChannel;

    public static void main(String[] args) throws Exception {
        ProxyServer proxyServer=new ProxyServer(9002, new ChannelInitializer() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("LengthFieldBasedFrameDecoder",new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,2,4,0,0,true))
                        .addLast("ReadTimeoutHandler",new ReadTimeoutHandler(30))
                        .addLast("HeaderIdentifyHandler",new HeaderIdentifyHandler())
                        .addLast("DestinationConnectHandler",new DestinationConnectHandler())
                .addLast("ExceptionLoggerHandler",new ExceptionLoggerHandler((ctx,cause)->ctx.channel().remoteAddress().toString()+":"+cause.toString()));
            }
        });
        proxyServer.serverChannel.closeFuture().addListener((f) -> {
            System.out.println("server stop");
            eventLoopGroup.shutdownGracefully();
        });

    }
}
