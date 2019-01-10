package Server;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetAddress;

public class EchoServer {
    public static void main(String[] args) throws Exception {
        ServerBootstrap serverBootstrap=new ServerBootstrap();
        serverBootstrap.group(ProxyServer.eventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInboundHandlerAdapter(){
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        System.out.println(ByteBufUtil.hexDump((ByteBuf) msg));
                        super.channelRead(ctx,msg);
                    }
                });
        InetAddress localAddr=InetAddress.getLocalHost();
        ChannelFuture future = serverBootstrap.bind(localAddr, 5559);
        future.syncUninterruptibly();
    }
}
