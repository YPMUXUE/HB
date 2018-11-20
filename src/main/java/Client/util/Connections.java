package Client.util;

import Client.bean.HostAndPort;
import Client.handler.AddDestinationhandler;
import Client.handler.AddHeaderHandler;
import Client.handler.AddLengthHandler;
import Client.handler.SimpleTransferHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.concurrent.Future;

import java.util.function.Consumer;

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
    public static ChannelFuture newConnectionToProxyServer(ChannelHandlerContext ctx, FullHttpRequest msg, Consumer<Future> futureConsumer){
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
                                .addLast("destination", new AddDestinationhandler(HostAndPort.resolve(msg)));

                    }
                });
        ChannelFuture future=bootstrap.connect("localhost",9002);
        return future;
    }
}
