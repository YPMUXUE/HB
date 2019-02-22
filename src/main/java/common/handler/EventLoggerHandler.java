package common.handler;

import common.log.LogUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.io.File;
import java.util.function.BiFunction;

public class EventLoggerHandler extends ChannelDuplexHandler {
    private final BiFunction<ChannelHandlerContext,Throwable,String> exceptionHandler;
    private final boolean logClose;
    public static final BiFunction<ChannelHandlerContext,Throwable,String> DEFAULT_HANDLER=new BiFunction<ChannelHandlerContext, Throwable, String>() {
        @Override
        public String apply(ChannelHandlerContext ctx, Throwable e) {
            return ctx.channel() + LogUtil.stackTraceToString(e);
        }
    };

    public EventLoggerHandler(BiFunction<ChannelHandlerContext, Throwable,String> exceptionHandler) {
        this(true,exceptionHandler);
    }
    public EventLoggerHandler(boolean logClose){
        this(logClose,DEFAULT_HANDLER);
    }
    public EventLoggerHandler(boolean logClose,BiFunction<ChannelHandlerContext, Throwable,String> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        this.logClose=logClose;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LogUtil.info(()->exceptionHandler.apply(ctx,cause));
        LogUtil.info(()->ctx.channel()+"start to close channel");
        ctx.channel().close().addListener(LogUtil.LOG_IF_FAILED);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        if (logClose) {
            LogUtil.info(() -> ctx.channel() + "closed.");
        }
        promise.addListener(LogUtil.LOG_IF_FAILED);
        super.close(ctx, promise);
    }
}
