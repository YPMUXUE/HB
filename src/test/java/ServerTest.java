import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetAddress;

public class ServerTest {
    public static void main(String[] args) throws Exception {
        ServerBootstrap bootstrap=new ServerBootstrap();
        EventLoopGroup eventLoopGroup=new NioEventLoopGroup();
        bootstrap.group(eventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new SoutChannelInboundHandler());
                    }
                });
        ChannelFuture future = bootstrap.bind(InetAddress.getByName("localhost"), 9001);
        Channel serverChannel=future.channel();
        serverChannel.closeFuture().addListener((f) -> {
            System.out.println("server stop");
            eventLoopGroup.shutdownGracefully();
        });
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            serverChannel.close().syncUninterruptibly();
        }));

    }

    public static class SoutChannelInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            byte[] buffer=new byte[msg.readableBytes()];
            msg.readBytes(buffer);
            System.out.println(new String(buffer));
        }
    }
}

