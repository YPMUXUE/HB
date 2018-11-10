package Client.handler;

import Client.bean.HostAndPort;
import Client.util.RequestResolveUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestEncoder;

import java.nio.charset.Charset;

public class HttpProxyHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final static ByteBuf CONNECT_RESPONSE_OK = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("HTTP/1.1 200 Connection Established\r\n\r\n", Charset.forName("utf-8")));

    public HttpProxyHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        if (HttpMethod.CONNECT.equals(msg.method())) {
            ChannelFuture channelFuture = connectRequest(ctx, msg);
            String hostName = msg.uri();
            channelFuture.addListener((f) -> {
                if (f.isSuccess()) {
                    System.out.println(hostName + "connect success");
                    ctx.pipeline().remove("HttpProxyHandler").handlerRemoved(ctx);
                    ctx.pipeline().addLast("ConnectMethodHandler",new ConnectMethodHandler(channelFuture.channel()));
                    ctx.channel().writeAndFlush(CONNECT_RESPONSE_OK);
                } else {
                    System.out.println(hostName + "connect failed");
                    ctx.channel().close();
                }
            });
        } else {

        }
    }

    private ChannelFuture connectRequest(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
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
        HostAndPort hostAndPort=RequestResolveUtil.resolveRequest(msg);
        ChannelFuture future = bootstrap.connect(hostAndPort.getHost(), hostAndPort.getPort());
        return future;
    }
}
