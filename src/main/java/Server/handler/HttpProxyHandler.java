package Server.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestEncoder;

import java.net.InetAddress;
import java.nio.charset.Charset;

public class HttpProxyHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final static String CONNECT_RESPONSE_OK ="HTTP/1.1 200 Connection Established\r\n\r\n";
    private ChannelFuture future;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        if (HttpMethod.CONNECT.equals(msg.method())){
            connectRequest(ctx,msg);
        }else{
            future.channel().writeAndFlush(msg.retain());
        }
    }

    private void connectRequest(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        Bootstrap bootstrap=new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class).handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext c, ByteBuf msg) throws Exception {
                        ctx.writeAndFlush(msg.retain());
                    }
                }).addLast(new HttpRequestEncoder());
            }
        });
        String uri=msg.uri();
        String host=uri.substring(0,uri.lastIndexOf(":"));
        int port=Integer.valueOf(uri.substring(uri.lastIndexOf(":")+1,uri.length()));
        this.future=bootstrap.connect(InetAddress.getByName(host),port);
        future.addListener((f)->{
            if (f.isSuccess()){
                System.out.println("connect success");
                ctx.writeAndFlush(Unpooled.copiedBuffer(CONNECT_RESPONSE_OK, Charset.forName("utf-8")));
            }else{
                System.out.println("connect failed");
                ctx.close();
            }
        });
    }
}
