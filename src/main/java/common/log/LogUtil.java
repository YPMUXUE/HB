package common.log;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Objects;
import java.util.function.Supplier;

public class LogUtil {
    private final static PrintStream logger;
    private static final boolean DEBUG;
    public static final ChannelFutureListener LOGGER_ON_FAILED_CLOSE;
    static {
        LOGGER_ON_FAILED_CLOSE =new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()){

                }else{
                    LogUtil.error(()->"channel:"+ future.channel() +"cause:"+stackTraceToString(future.cause()));
                    future.channel().close();
                }
            }
        };
        File logFile=new File("/HB/log/logFile");
        PrintStream logStream;
        try {
            logFile.getParentFile().mkdirs();
            logFile.createNewFile();
            logStream = new PrintStream(logFile, "utf-8");
        }catch (Exception e){
            logStream=System.out;
            logStream.println("using system.out");
        }
        logger=logStream;
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
