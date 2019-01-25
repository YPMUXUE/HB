package common.util;

import common.handler.ReadWriteTimeoutHandler;
import common.handler.coder.ByteBufToMessageInboundHandler;
import common.handler.coder.MessageToByteBufOutboundHandler;
import config.StaticConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.concurrent.Future;
import common.resource.SystemConfig;

import java.net.SocketAddress;
import java.util.function.BiConsumer;


public class Connections {
    private static final Bootstrap defaultBootstrap=new Bootstrap();
    public static ChannelFuture newConnectionToServer(EventLoop eventLoop, SocketAddress address, BiConsumer<Integer,Channel> channelConsumer) throws Exception{
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoop)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                     ch.pipeline().addLast("ReadWriteTimeoutHandler",new ReadWriteTimeoutHandler(SystemConfig.timeout));
                    }
                });
        ChannelFuture future = bootstrap.connect(address);
        Channel channelToServer=future.channel();
        future.addListener((f)->{
            if (f.isSuccess()){
                channelConsumer.accept(1,channelToServer);
            }else{
                channelConsumer.accept(0,channelToServer);
            }
        });
        return future;
    }

    /**
     *
     */
    public static ChannelFuture newConnectionToProxyServer(EventLoop eventLoop, BiConsumer<Future,Channel> channelConsumer){
        Bootstrap bootstrap = defaultBootstrap.clone();
        bootstrap.group(eventLoop)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        //inbound
                        ch.pipeline().addLast("LengthFieldBasedFrameDecoder",HandlerHelper.getDefaultFrameDecoderInstance())
                                .addLast("ByteBufToMessageHandler",new ByteBufToMessageInboundHandler());
                        //outbound
                        ch.pipeline().addLast("MessageToByteBufHandler",new MessageToByteBufOutboundHandler());

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
