package common.handler;

import common.log.LogUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.function.BiFunction;

public class EventLoggerHandler extends ChannelInboundHandlerAdapter {
    private final BiFunction<ChannelHandlerContext,Throwable,String> exceptionHandler;
    public EventLoggerHandler(BiFunction<ChannelHandlerContext, Throwable,String> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LogUtil.info(()->exceptionHandler.apply(ctx,cause));
        ctx.channel().close();
    }
}
