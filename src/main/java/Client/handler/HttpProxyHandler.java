package Client.handler;

import Client.log.LogUtil;
import Client.util.ConnectionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

public class HttpProxyHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final static ByteBuf CONNECT_RESPONSE_OK = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("HTTP/1.1 200 Connection Established\r\n\r\n", Charset.forName("utf-8")));

    public HttpProxyHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        if(!msgInterested(msg)){
            ctx.fireChannelRead(msg);
            return;
        }
        LogUtil.debug(msg::toString);
        ChannelFuture clientToServerChannelFuture;
            clientToServerChannelFuture = ConnectionUtil.newConnectionToServer(ctx, msg, new ChannelInitializer() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addLast(new SimpleTransferHandler(ctx.channel()))
                            .addLast("ExceptionHandler",new ExceptionLoggerHandler("ClientToServer"));
                }
            });
            String hostName = msg.uri();
            clientToServerChannelFuture.addListener((f) -> {
                if (f.isSuccess()) {
                    LogUtil.info(()->(hostName + "connect success"));
                    //删除所有RequestToClient下ChannelHandler
                    ctx.pipeline().forEach((entry)->ctx.pipeline().remove(entry.getKey()));
                    ctx.pipeline().addLast("ReadTimeoutHandler",new ReadTimeoutHandler(15, TimeUnit.SECONDS))
                            .addLast("ConnectMethodHandler",new ConnectMethodHandler(clientToServerChannelFuture.channel()))
                            .addLast("ExceptionHandler",new ExceptionLoggerHandler("HttpProxyHandler"));
                    ctx.channel().writeAndFlush(CONNECT_RESPONSE_OK);
                } else {
                    LogUtil.info(()->(hostName + "connect failed"));
                    ctx.channel().close();
                }
            });

    }

    private boolean msgInterested(FullHttpRequest msg) {
        return HttpMethod.CONNECT.equals(msg.method());
    }

}
