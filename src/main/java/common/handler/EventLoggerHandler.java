package common.handler;

import common.log.LogUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.function.BiFunction;

public class EventLoggerHandler extends ChannelDuplexHandler {
    private final BiFunction<ChannelHandlerContext,Throwable,String> exceptionHandler;
    public EventLoggerHandler(BiFunction<ChannelHandlerContext, Throwable,String> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LogUtil.info(()->exceptionHandler.apply(ctx,cause));
        ctx.channel().close().addListener(LogUtil.LOGGER_ON_FAILED_CLOSE);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        LogUtil.info(()->ctx.channel()+ "closed.");
        promise.addListener(LogUtil.LOGGER_ON_FAILED_CLOSE);
        super.close(ctx, promise);
    }
}
