package priv.common.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import priv.common.log.LogUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.function.BiFunction;

public class EventLoggerHandler extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(EventLoggerHandler.class);
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
        logger.error(moduleName + " : " + exceptionHandler.apply(ctx, cause));
        logger.info(moduleName+ " current stack trace:",new Exception("stack trace"));
        ctx.channel().close();
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        if (logClose) {
            logger.info(moduleName + " : " + ctx.channel() + "call close.");
        }
//         promise.addListener(LogUtil.LOG_IF_FAILED);
        super.close(ctx, promise);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info(moduleName + " : " + ctx.channel() + "channel Inactive.");
        super.channelInactive(ctx);
    }
}
