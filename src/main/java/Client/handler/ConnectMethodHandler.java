package Client.handler;

import Client.bean.HostAndPort;
import Client.log.LogUtil;
import Client.util.Connections;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

public class ConnectMethodHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final static ByteBuf CONNECT_RESPONSE_OK = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("HTTP/1.1 200 Connection Established\r\n\r\n", Charset.forName("utf-8")));

    public ConnectMethodHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        if(!msgInterested(msg)){
            ctx.fireChannelRead(msg);
            return;
        }
        LogUtil.debug(msg::toString);
        String hostName = msg.uri();
         Connections.newConnectionToProxyServer(ctx.channel().eventLoop(),HostAndPort.resolve(msg),(channelFuture, channelToProxyServer)->{
            if (channelFuture.isSuccess()){
                LogUtil.info(()->(hostName + "connect success"));

                //删除所有RequestToClient下ChannelHandler
                ctx.pipeline().forEach((entry)->ctx.pipeline().remove(entry.getKey()));
                ctx.pipeline().addLast("ReadTimeoutHandler",new ReadTimeoutHandler(15, TimeUnit.SECONDS))
                        .addLast("SimpleTransferHandler",new SimpleTransferHandler(channelToProxyServer))
                        .addLast("ExceptionHandler",new ExceptionLoggerHandler("ConnectMethodHandler"));
                channelToProxyServer.writeAndFlush(Unpooled.EMPTY_BUFFER);
            }else{
                LogUtil.info(()->(hostName + "connect failed"));
                ctx.channel().close();
            }
        });

    }

    private boolean msgInterested(FullHttpRequest msg) {
        return HttpMethod.CONNECT.equals(msg.method());
    }

}
