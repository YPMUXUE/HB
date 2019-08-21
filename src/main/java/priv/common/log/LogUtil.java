package priv.common.log;

import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Objects;
import java.util.function.Supplier;

public class LogUtil {
    private final static PrintStream logger;

    static {
        logger = System.out;
        System.setErr(logger);
    }

    public static  ChannelFutureListener LOG_IF_FAILED(Logger logger){
        return future -> {
            if (!future.isSuccess()){
                logger.error("channel:"+ future.channel() +"result:"+stackTraceToString(future.cause()));
            }
        };
    }

    public static ChannelFutureListener LOGGER_ON_FAILED_CLOSE(Logger logger){
        return future -> {
            if (future.isSuccess()){

            }else{
                logger.error("channel:"+ future.channel() +"cause:"+stackTraceToString(future.cause()));
                future.channel().close();
            }
        };
    }
    public static void info(Supplier<String> s){
        Objects.requireNonNull(s);
        logger.println("info:"+s.get());
    }

    public static void error(Supplier<String> s) {
        logger.println("error:" + Objects.requireNonNull(s).get());
    }
    public static String stackTraceToString(Throwable cause){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream pout = new PrintStream(out);
        cause.printStackTrace(pout);
        pout.flush();
        try {
            return new String(out.toByteArray());
        } finally {
            try {
                out.close();
            } catch (IOException ignore) {
                // ignore as should never happen
            }
        }
    }
}
