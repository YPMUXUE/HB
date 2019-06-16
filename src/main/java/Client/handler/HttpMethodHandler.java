package Client.handler;

import Client.bean.HostAndPort;
import common.Message;
import common.handler.EventLoggerHandler;
import common.log.LogUtil;
import common.resource.StaticConfig;
import common.resource.SystemConfig;
import common.util.Connections;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.concurrent.TimeUnit;

@Deprecated
public class HttpMethodHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final static String clientTransferHandler ="ClientTransferHandler";
    private final static String proxyTransferHandler="ProxyTransferHandler";
    private Channel ChannelToProxyServer;

    public HttpMethodHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        LogUtil.debug(msg::toString);
        try {
            if (HttpMethod.CONNECT.equals(msg.method())) {
                handleConnect(ctx, msg);
            } else {
                //TODO 以后有心情了再考虑
//                handleSimpleProxy(ctx, msg);
                ctx.channel().close();
                return;
            }
        }finally {
//            这里不需要释放，SimpleChannelInboundHandler里有释放操作 巨坑注意
//            msg.release();
        }
    }

    private void handleSimpleProxy(ChannelHandlerContext ctx, FullHttpRequest msg) {
        throw new UnsupportedOperationException("普通HTTP代理请求还没完成，先放着");
//        String uri=msg.uri();
//        String host=msg.headers().get(HttpHeaderNames.HOST);
//        LogUtil.info(()->("uri:"+uri+",host:"+host));
//        final HostAndPort destination = HostAndPort.resolve(msg);
//        final Message reqMessage= MessageUtil.HttpRequestToMessage(Unpooled.buffer(),msg);
//        msg.release();
//        PendingWriteTransferHandler oldProxyHandler=(PendingWriteTransferHandler)ctx.pipeline().get(proxyTransferHandler);
//        if (oldProxyHandler == null){
//            Connections.newConnectionToProxyServer(ctx.channel().eventLoop(),(channelFuture, channelToProxyServer)->{
////                ctx.pipeline().addLast(proxyTransferHandler,new PendingWriteTransferHandler())
//            });
//        }else {
//            PendingWriteTransferHandler newProxyHandler = new PendingWriteTransferHandler(oldProxyHandler, destination);
//        }

    }

    private void handleConnect(ChannelHandlerContext ctx, FullHttpRequest msg) {
        String hostName = msg.uri();
        HostAndPort destination = HostAndPort.resolve(msg);
        Connections.newConnectionToProxyServer(ctx.channel().eventLoop(), (channelFuture, channelToProxyServer) -> {
            if (channelFuture.isSuccess()) {
                LogUtil.info(() -> ("Proxy Server:" + StaticConfig.PROXY_SERVER_ADDRESS + " connect success, bind address" + hostName));

                //删除所有RequestToClient下ChannelHandler
                ctx.pipeline().forEach((entry) -> ctx.pipeline().remove(entry.getKey()));
                ctx.pipeline().addLast("ReadTimeoutHandler", new ReadTimeoutHandler(SystemConfig.timeout, TimeUnit.SECONDS))
                        .addLast(clientTransferHandler, new ClientTransferHandler(channelToProxyServer, true, ClientTransferHandler.HTTPS_PROCESSOR))
                        .addLast("EventLoggerHandler", new EventLoggerHandler("ClientToProxyServer", true));

                channelToProxyServer.pipeline().addLast(proxyTransferHandler, new ProxyTransferHandler(ctx.channel(), true, destination));
            } else {
                LogUtil.info(() -> (hostName + "connect failed"));
                ctx.channel().close();
            }
        });
    }
    private Message PackageMessageToProxyServer(FullHttpRequest req){
        return null;
    }
}
