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
    private final static ByteBuf CONNECT_RESPONSE_OK = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("HTTP/1.1 200 Connection Established\r\n\r\n", Charset.forName("utf-8")));
    private ChannelFuture clientFuture;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        if (HttpMethod.CONNECT.equals(msg.method())) {
            this.clientFuture = connectRequest(ctx, msg);
            String hostName = msg.uri();
            clientFuture.addListener((f) -> {
                if (f.isSuccess()) {
                    System.out.println(hostName + "connect success");
                    ctx.writeAndFlush(CONNECT_RESPONSE_OK);
                } else {
                    System.out.println(hostName + "connect failed");
                    ctx.close();
                }
            });
        } else {
            if (this.clientFuture == null) {
                //todo future为null说明之前没有发送过CONNECT请求，可能是个普通HTTP或proxy-Connection请求
                ctx.close();
            } else {
                if(this.clientFuture.isDone()) {
                    this.clientFuture.channel().writeAndFlush(msg.retain());
                }else{
                    this.clientFuture.addListener((f)->{
                        if (f.isSuccess()){
                            clientFuture.channel().write(msg);
                        }else{

                        }
                    })
                }
            }
        }
    }

    private ChannelFuture connectRequest(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        Bootstrap bootstrap = new Bootstrap();
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
        String uri = msg.uri();
        String host = uri.substring(0, uri.lastIndexOf(":"));
        int port = Integer.valueOf(uri.substring(uri.lastIndexOf(":") + 1, uri.length()));
        ChannelFuture future = bootstrap.connect(InetAddress.getByName(host), port);
        return future;
    }
}
