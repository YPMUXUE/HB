package priv.Server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class EchoServer {
    public static void main(String[] args) throws Exception {
        ServerBootstrap serverBootstrap=new ServerBootstrap();
        serverBootstrap.group(ProxyServer.eventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {

                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                System.out.println(new String(ByteBufUtil.getBytes(((ByteBuf)msg).retain()), StandardCharsets.UTF_8));
                                System.out.println(ByteBufUtil.hexDump((ByteBuf) msg));
                                super.channelRead(ctx,msg);
                            }

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                super.channelActive(ctx);
                            }
                        });
                    }
                });
        InetAddress localAddr=InetAddress.getLocalHost();
        SocketAddress address=new InetSocketAddress("127.0.0.1",4449);
        ChannelFuture future = serverBootstrap.bind(address);
        future.syncUninterruptibly();
    }
}
