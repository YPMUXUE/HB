package Client.util;

import Client.bean.HostAndPort;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;

public class ConnectionUtil {
    public static ChannelFuture newConnectionToServer(ChannelHandlerContext ctx, FullHttpRequest msg, ChannelInitializer channelInitializer) throws Exception{
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(channelInitializer);
        HostAndPort hostAndPort=HostAndPort.resolve(msg);
        ChannelFuture future = bootstrap.connect(hostAndPort.getHost(), hostAndPort.getPort());
        return future;
    }
}
