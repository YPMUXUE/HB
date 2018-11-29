package common.log;

import java.io.PrintStream;
import java.util.Objects;
import java.util.function.Supplier;

public class LogUtil {
    private final static PrintStream logger=System.out;
    private static final boolean DEBUG;

    static {DEBUG=Boolean.valueOf(System.getProperty("debug","false"));}

    public static void info(Supplier<String> s){
        Objects.requireNonNull(s);
        logger.println(s.get());
    }
    public static void debug(Supplier<String> s){
        if (DEBUG) {
            info(s);
        }
    }

}
