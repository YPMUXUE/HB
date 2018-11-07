import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetAddress;
import java.nio.charset.Charset;

public class ClientTest {
    public static void main(String[] args) throws Exception {
        Bootstrap bootstrap=new Bootstrap();
        EventLoopGroup eventLoopGroup=new NioEventLoopGroup();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new SimpleChannelInboundHandler<ByteBuf>() {
                    private int i=0;
                    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                        byte[] buffer=new byte[msg.readableBytes()];
                        msg.readBytes(buffer);
//                        System.out.println(new String(buffer,0,buffer.length,"UTF-8"));
                        System.out.println(ByteBufUtil.hexDump(buffer));
                    }

                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        ctx.writeAndFlush(Unpooled.copiedBuffer("GET / HTTP/1.1\r\nHost:www.baidu.com\r\n\r\n",Charset.forName("UTF-8")));
                        super.channelActive(ctx);
                    }
                });
        ChannelFuture future1 = bootstrap.connect(InetAddress.getByName("www.baidu.com"), 80).addListener((future) -> {
            if (future.isSuccess()) {
                System.out.println("connect success");
            }
        });
        future1.channel().closeFuture().addListener((future -> eventLoopGroup.shutdownGracefully()));


    }
}
