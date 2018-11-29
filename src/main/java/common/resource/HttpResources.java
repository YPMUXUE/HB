package common.resource;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;

public class HttpResources {
    static public class HttpRequest{

    }
    static public class HttpResponse{
        public static final String Connection_Established="HTTP/1.1 200 Connection Established\r\n\r\n";
    }
}
