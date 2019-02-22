package Client.handler;

import Client.bean.HostAndPort;
import common.handler.EventLoggerHandler;
import common.log.LogUtil;
import common.resource.SystemConfig;
import common.util.Connections;
import common.resource.StaticConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.concurrent.TimeUnit;

public class HttpMethodHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final static String clientTransferHandler ="ClientTransferHandler";
    private final static String proxyTransferHandler="ProxyTransferHandler";

    public HttpMethodHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        LogUtil.debug(msg::toString);
        try {
            if (HttpMethod.CONNECT.equals(msg.method())) {
                handleConnect(ctx, msg);
            } else {
                handleSimpleProxy(ctx, msg);
            }
        }finally {
//            这里不需要释放，SimpleChannelInboundHandler里有释放操作 巨坑注意
//            msg.release();
        }
    }

    private void handleSimpleProxy(ChannelHandlerContext ctx, FullHttpRequest msg) {
//        msg.
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
                        .addLast(clientTransferHandler, new ClientTransferHandler(channelToProxyServer, true))
                        .addLast("EventLoggerHandler", new EventLoggerHandler((context,cause)->"ClientToProxyServer:" + EventLoggerHandler.DEFAULT_HANDLER.apply(context,cause)));

                channelToProxyServer.pipeline().addLast(proxyTransferHandler, new ProxyTransferHandler(ctx.channel(), true, destination));
            } else {
                LogUtil.info(() -> (hostName + "connect failed"));
                ctx.channel().close().addListener(LogUtil.LOG_IF_FAILED);
            }
        });
    }
}
