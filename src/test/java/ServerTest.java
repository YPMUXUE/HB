import common.resource.ConnectionEvents;
import config.StaticConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

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
//                        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,2,4));
                        pipeline.addLast(new SoutChannelInboundHandler());
                    }
                });
        ChannelFuture future = bootstrap.bind(InetAddress.getLoopbackAddress(), StaticConfig.PROXY_SERVER_PORT);
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
//            HttpResponse(ctx,msg);
            System.out.println(ByteBufUtil.hexDump(msg));
            ctx.channel().writeAndFlush(Unpooled.buffer().writeShort(ConnectionEvents.CONNECTION_ESTABLISH.getCode()).writeInt(0));
            }

        private void HttpResponse(ChannelHandlerContext ctx, ByteBuf msg) {
            System.out.println(msg.toString(StandardCharsets.UTF_8));
            String message="HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/json; charset=utf-8\r\n" +
                    "\r\n" +
                    "{\"isLogin\":false,\"username\":\"\",\"userId\":\"\",\"timestamp\":1542701358812}\r\n";
            ctx.channel().write(Unpooled.buffer().writeBytes(new byte[]{(byte)0xCA,(byte)0xFE}).writeInt(message.getBytes(StandardCharsets.UTF_8).length));
            ctx.writeAndFlush(Unpooled.copiedBuffer(message, Charset.forName("utf-8")));

        }
    }
}

