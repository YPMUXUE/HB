package Client.handler;

import Client.bean.HostAndPort;
import Client.log.LogUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
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
        LogUtil.debug(msg::toString);
        if (HttpMethod.CONNECT.equals(msg.method())) {
            ChannelFuture clientToServerChannelFuture = connectRequest(ctx, msg, new ForwardToServerHandler(ctx.channel()));
            String hostName = msg.uri();
            clientToServerChannelFuture.addListener((f) -> {
                if (f.isSuccess()) {
                    LogUtil.info(()->(hostName + "connect success"));
                    //删除所有RequestToClient下ChannelHandler
                    ctx.pipeline().forEach((entry)->ctx.pipeline().remove(entry.getKey()));
                    ctx.pipeline().addLast("ConnectMethodHandler",new ConnectMethodHandler(clientToServerChannelFuture.channel()))
                            .addLast("ReadTimeoutHandler",new ReadTimeoutHandler(15, TimeUnit.SECONDS))
                    .addLast("ExceptionHandler",new ExceptionLoggerHandler("HttpProxyHandler"));
                    ctx.channel().writeAndFlush(CONNECT_RESPONSE_OK);
                } else {
                    LogUtil.info(()->(hostName + "connect failed"));
                    ctx.channel().close();
                }
            });
        } else {
            System.out.println(ctx.channel().toString());
            ChannelFuture clientToServerChannelFuture = connectRequest(ctx, msg, new ChannelInitializer() {
                @Override
                protected void initChannel(Channel ch) throws Exception {

                }
            });
            String hostName = msg.uri();
            clientToServerChannelFuture.addListener((f)->{
                if (f.isSuccess()){
                    LogUtil.info(()->(hostName + " connect success"));

                }else{
                    LogUtil.info(()->hostName+" connect failed");
                }
            });

        }
    }


    private ChannelFuture connectRequest(ChannelHandlerContext ctx, FullHttpRequest msg, ChannelInitializer initializer) throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(initializer);
        HostAndPort hostAndPort=HostAndPort.resolve(msg);
        ChannelFuture future = bootstrap.connect(hostAndPort.getHost(), hostAndPort.getPort());
        return future;
    }
}
