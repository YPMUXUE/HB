package Client.handler;

import Client.bean.HostAndPort;
import Client.log.LogUtil;
import Client.util.RequestResolveUtil;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
                    LogUtil.info(()->(hostName + "connect success"));
                    //删除所有channelHandler
                    ctx.pipeline().forEach((entry)->ctx.pipeline().remove(entry.getKey()));
                    ctx.pipeline().addLast("ConnectMethodHandler",new ConnectMethodHandler(channelFuture.channel()))
                            .addLast("ReadTimeoutHandler",new ReadTimeoutHandler(15,TimeUnit.SECONDS));
                    ctx.channel().writeAndFlush(CONNECT_RESPONSE_OK);
                } else {
                    LogUtil.info(()->(hostName + "connect failed"));
                    ctx.channel().close();
                }
            });
        } else {
            System.out.println(msg);

        }
    }


    private ChannelFuture connectRequest(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext c, ByteBuf msg) {
                        ctx.channel().writeAndFlush(msg.retain());
                    }
                });
            }
        });
        HostAndPort hostAndPort=HostAndPort.resolve(msg);
        ChannelFuture future = bootstrap.connect(hostAndPort.getHost(), hostAndPort.getPort());
        return future;
    }
}
