package priv.common.util;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import priv.common.handler.ReadWriteTimeoutHandler;
import priv.common.resource.StaticConfig;
import priv.common.resource.SystemConfig;

import java.net.SocketAddress;
import java.util.function.BiConsumer;


public class Connections {
    private static final Bootstrap defaultBootstrap=new Bootstrap();
    public static final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    public static ChannelFuture connect(EventLoop eventLoop, SocketAddress address, ChannelInitializer channelInitializer){
        Bootstrap bootstrap = defaultBootstrap.clone();
        bootstrap.group(eventLoop == null ?  eventLoopGroup: eventLoop)
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

}
