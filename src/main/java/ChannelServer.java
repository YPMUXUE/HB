import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import java.net.InetAddress;
import java.nio.charset.Charset;

public class ChannelServer {
    protected static final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    protected Channel serverChannel;

    public ChannelServer(String host, int port, ChannelInitializer channelInitializer) throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(channelInitializer);
        ChannelFuture future = bootstrap.bind(InetAddress.getByName(host), port);
        future.addListener(f->{
            if (f.isSuccess()){
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    serverChannel.close().syncUninterruptibly();
                }));
            }
        });
        future.syncUninterruptibly();
        this.serverChannel = future.channel();
    }

    public static void main(String[] args) throws Exception {
        ChannelServer channelServer=new ChannelServer("localhost", 9001, new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new HttpServerCodec());
                pipeline.addLast(new HttpObjectAggregator(64*1024));
                pipeline.addLast(new SoutChannelInboundHandler());
            }
        });
        channelServer.serverChannel.closeFuture().addListener((f) -> {
            System.out.println("server stop");
            eventLoopGroup.shutdownGracefully();
        });


    }

    public static class SoutChannelInboundHandler extends SimpleChannelInboundHandler {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object m) throws Exception {
            if (m instanceof ByteBuf) {
                ByteBuf msg=(ByteBuf)m;
                byte[] buffer = new byte[msg.readableBytes()];
                msg.readBytes(buffer);
                System.out.println(new String(buffer));
            }else if(m instanceof FullHttpRequest){
                System.out.println(m.toString());
                eventLoopGroup.execute();
            }
        }
    }
}
