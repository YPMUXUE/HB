package priv.common.util;

import priv.common.handler.EventLoggerHandler;
import priv.common.handler.ReadWriteTimeoutHandler;
import priv.common.handler.coder.ByteBufToMessageInboundHandler;
import priv.common.handler.coder.MessageToByteBufOutboundHandler;
import priv.common.resource.StaticConfig;
import priv.common.resource.SystemConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;

import java.net.SocketAddress;
import java.util.function.BiConsumer;


public class Connections {
    private static final Bootstrap defaultBootstrap=new Bootstrap();
    static final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    public static ChannelFuture connect(EventLoop eventLoop, SocketAddress address, ChannelInitializer channelInitializer){
        Bootstrap bootstrap = defaultBootstrap.clone();
        bootstrap.group(eventLoop)
                .channel(NioSocketChannel.class)
                .handler(channelInitializer);
        return bootstrap.connect(address);
    }

    @Deprecated
    public static ChannelFuture newConnectionToServer(EventLoop eventLoop, SocketAddress address, BiConsumer<Integer,Channel> channelConsumer) throws Exception{
        Bootstrap bootstrap = defaultBootstrap.clone();
        bootstrap.group(eventLoop)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                     ch.pipeline().addLast("ReadWriteTimeoutHandler",new ReadWriteTimeoutHandler(StaticConfig.timeout));
                    }
                });
        ChannelFuture future = bootstrap.connect(address);
        Channel channelToServer=future.channel();
        future.addListener((f)->{
            if (f.isSuccess()){
                channelConsumer.accept(SystemConfig.SUCCESS,channelToServer);
            }else{
                channelConsumer.accept(SystemConfig.FAILED,channelToServer);
            }
        });
        return future;
    }

    @Deprecated
    public static ChannelFuture newConnectionToProxyServer(EventLoop eventLoop, BiConsumer<Future,Channel> channelConsumer){
        Bootstrap bootstrap = defaultBootstrap.clone();
        bootstrap.group(eventLoop)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        //inbound
                        ch.pipeline().addLast("LengthFieldBasedFrameDecoder",HandlerHelper.newDefaultFrameDecoderInstance())
                                .addLast("ByteBufToMessageHandler",new ByteBufToMessageInboundHandler());
                        //outbound
                        ch.pipeline().addLast("MessageToByteBufHandler",new MessageToByteBufOutboundHandler());
                        //log
                        ch.pipeline().addLast("EventLoggerHandler",new EventLoggerHandler("ConnectionToProxyServer", false));
                    }
                });
        ChannelFuture future=bootstrap.connect(StaticConfig.PROXY_SERVER_ADDRESS,StaticConfig.PROXY_SERVER_PORT);
        Channel channel=future.channel();
        future.addListener((f)->{
            if (f.isSuccess()){
                channelConsumer.accept(f,channel);
            }else{
                channelConsumer.accept(f,channel);
            }
        });
        return future;
    }
}