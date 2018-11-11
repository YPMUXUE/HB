import Client.bean.HostAndPort;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

public class MainTest {
    public static void main(String[] args) {
        DefaultFullHttpRequest defaultFullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
//        defaultFullHttpRequest.headers().add("Host","www.baidu.com:443");
        HostAndPort.resolve(defaultFullHttpRequest);
        System.out.println("...");
    }
}
