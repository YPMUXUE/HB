package Server.handler;

import log.LogUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.function.BiFunction;

public class ExceptionLoggerHandler extends ChannelInboundHandlerAdapter {
    private final BiFunction<ChannelHandlerContext,Throwable,String> exceptionHandler;
    public ExceptionLoggerHandler(BiFunction<ChannelHandlerContext, Throwable,String> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LogUtil.info(()->exceptionHandler.apply(ctx,cause));
    }
}
