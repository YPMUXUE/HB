package Client.handler;

import common.log.LogUtil;
import common.util.Connections;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;

public class HTTPMethodHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        FullHttpRequest req= (FullHttpRequest) msg;
        boolean keepAlive=false;
        if (req.headers().contains("Proxy-Connection")){
            keepAlive=req.headers().get("Proxy-Connection").equals("keep-alive");
            req.headers().remove("Proxy-Connection");
        }
        Connections.newConnectionToProxyServer(ctx.channel().eventLoop(),(future,channel)->{
            if (future.isSuccess()){
                LogUtil.info(()->channel.toString()+"Connect success");

            }
        })
    }
}
