package Client.util;

import Client.bean.HostAndPort;
import Client.handler.AddDestinationHandler;
import Client.handler.AddHeaderHandler;
import Client.handler.AddLengthHandler;
import Client.handler.SimpleTransferHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.function.BiConsumer;


public class Connections {
    public static ChannelFuture newConnectionToServer(ChannelHandlerContext ctx, FullHttpRequest msg, ChannelInitializer channelInitializer) throws Exception{
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(channelInitializer);
        HostAndPort hostAndPort=HostAndPort.resolve(msg);
        ChannelFuture future = bootstrap.connect(hostAndPort.getHost(), hostAndPort.getPort());
        return future;
    }
    public static Channel newConnectionToProxyServer(ChannelHandlerContext ctx, FullHttpRequest msg, BiConsumer<Integer,Channel> channelConsumer){
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        //inbound
                        ch.pipeline().addLast("LengthFieldBasedFrameDecoder",new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,2,4,0,6,true))
                                .addLast("Transfer",new SimpleTransferHandler(ctx.channel()));
                        //outbound
                        ch.pipeline().addLast("header",new AddHeaderHandler())
                                .addLast("length",new AddLengthHandler())
                                .addLast("destination", new AddDestinationHandler(HostAndPort.resolve(msg)));

                    }
                });
        ChannelFuture future=bootstrap.connect("192.168.220.1",9002);
        Channel channel=future.channel();
        future.addListener((f)->{
            if (f.isSuccess()){
                channelConsumer.accept(1,channel);
            }else{
                channelConsumer.accept(0,channel);
            }
        });
        return channel;
    }
}
