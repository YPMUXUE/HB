import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

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
        SocketAddress address=new InetSocketAddress(InetAddress.getLoopbackAddress(),9001);
        ChannelFuture future = serverBootstrap.bind(address);
        future.syncUninterruptibly();
    }
    public static class EchoHandler extends ChannelDuplexHandler {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println(ByteBufUtil.hexDump((ByteBuf) msg));
            ((ByteBuf) msg).release();
        }
    }
}

