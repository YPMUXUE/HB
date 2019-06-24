import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import priv.common.resource.StaticConfig;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;

public class ConsoleOutputServer {
    public static void main(String[] args) throws Exception {
        ServerBootstrap serverBootstrap=new ServerBootstrap();
        EventLoopGroup eventLoopGroup=new NioEventLoopGroup();

        serverBootstrap.group(eventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast("ehco",new EchoHandler());
            }
        });
        InetAddress host=InetAddress.getLocalHost();
        SocketAddress address=new InetSocketAddress(StaticConfig.LOCAL_HOST_ADDRESS,8080);
        ChannelFuture future = serverBootstrap.bind(address);
        future.syncUninterruptibly();
    }
    public static class EchoHandler extends ChannelDuplexHandler {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println(((ByteBuf)msg).toString(Charset.forName("ascii")));
            ((ByteBuf) msg).release();
        }
    }
}

