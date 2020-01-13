package priv.common.handler;

import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import priv.common.log.LogUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.BiFunction;

public class EventLoggerHandler extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(EventLoggerHandler.class);
    private final BiFunction<ChannelHandlerContext,Throwable,String> exceptionHandler;
    private final String moduleName;
    private Collection<Class> ignoreStackTrace = new HashSet<>(Arrays.asList(TooLongFrameException.class, ReadTimeoutException.class, WriteTimeoutException.class));
    public static final BiFunction<ChannelHandlerContext,Throwable,String> DEFAULT_HANDLER=new BiFunction<ChannelHandlerContext, Throwable, String>() {
        @Override
        public String apply(ChannelHandlerContext ctx, Throwable e) {
            return ctx.channel() + LogUtil.stackTraceToString(e);
        }
    };

    public EventLoggerHandler(String moduleName){
        this(moduleName, DEFAULT_HANDLER);
    }
    public EventLoggerHandler(String moduleName, BiFunction<ChannelHandlerContext, Throwable, String> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        this.moduleName = moduleName == null ? "" : moduleName;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (logger.isErrorEnabled()) {
            logger.error("{}:{}", moduleName, exceptionHandler.apply(ctx, cause));
            Class c = cause.getClass();
            if (!ignoreStackTrace.contains(c)) {
                logger.error("{} current stack trace:{}",ctx.channel(), LogUtil.stackTraceToString(new Exception("stack trace")));
            }
        }
        ctx.channel().close();
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        logger.info("{}:{},call close", moduleName, ctx.channel().toString());
//         promise.addListener(LogUtil.LOG_IF_FAILED);
        super.close(ctx, promise);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("{}:{},channel Inactive", moduleName, ctx.channel().toString());
        super.channelInactive(ctx);
    }

    public void setIgnoreStackTrace(Collection<Class> ignoreStackTrace) {
        this.ignoreStackTrace = ignoreStackTrace;
    }
}
