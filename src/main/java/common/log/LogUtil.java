package common.log;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.io.PrintStream;
import java.util.Objects;
import java.util.function.Supplier;

public class LogUtil {
    private final static PrintStream logger=System.out;
    private static final boolean DEBUG;
    public static final ChannelFutureListener LOG_FUTURE_CLOSE_ON_FAILED;
    static {
        LOG_FUTURE_CLOSE_ON_FAILED=new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()){

                }else{
                    LogUtil.error(()->"channel:"+future.channel().toString()+"cause:"+future.cause().toString());
                    future.channel().close();
                }
            }
        };
    }

    static {DEBUG=Boolean.valueOf(System.getProperty("debug","false"));}

    public static void info(Supplier<String> s){
        Objects.requireNonNull(s);
        logger.println("info:"+s.get());
    }
    public static void debug(Supplier<String> s){
        if (DEBUG) {
            logger.println("debug:" + Objects.requireNonNull(s).get());
        }
    }

    public static void error(Supplier<String> s) {
        logger.println("error:" + Objects.requireNonNull(s).get());
    }
}
