package Client.log;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class LogUtil {
    private final static PrintStream logger=System.out;

    public static void info(Supplier<String> s){
        Objects.requireNonNull(s);
        logger.println(s.get());
    }
}
