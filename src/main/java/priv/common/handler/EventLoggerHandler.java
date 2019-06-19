package priv.common.handler;

import priv.common.log.LogUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.function.BiFunction;

public class EventLoggerHandler extends ChannelDuplexHandler {
    private final BiFunction<ChannelHandlerContext,Throwable,String> exceptionHandler;
    private final boolean logClose;
    private final String moduleName;
    public static final BiFunction<ChannelHandlerContext,Throwable,String> DEFAULT_HANDLER=new BiFunction<ChannelHandlerContext, Throwable, String>() {
        @Override
        public String apply(ChannelHandlerContext ctx, Throwable e) {
            return ctx.channel() + LogUtil.stackTraceToString(e);
        }
    };

    public EventLoggerHandler(String moduleName, boolean logClose){
        this(moduleName,logClose,DEFAULT_HANDLER);
    }
    public EventLoggerHandler(String moduleName, boolean logClose,BiFunction<ChannelHandlerContext, Throwable,String> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        this.logClose=logClose;
        this.moduleName = moduleName == null ? "" : moduleName;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LogUtil.info(() -> moduleName + " : " + exceptionHandler.apply(ctx, cause));
        LogUtil.info(() -> moduleName + " : " + ctx.channel() + "start to close channel");
        ctx.channel().close().addListener(LogUtil.LOG_IF_FAILED);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        if (logClose) {
            LogUtil.info(() -> moduleName + " : " + ctx.channel() + "call close.");
        }
        promise.addListener(LogUtil.LOG_IF_FAILED);
        super.close(ctx, promise);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }
}
