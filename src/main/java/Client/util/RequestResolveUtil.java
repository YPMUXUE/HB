package Client.util;

import Client.bean.HostAndPort;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;

public class RequestResolveUtil {
    public static HostAndPort resolveRequest(FullHttpRequest req) throws Exception{
        if (HttpMethod.CONNECT.equals(req.method())){
            return resolveConnectRequest(req);
        }else {
            return resolvePlainRequest(req);
        }

    }

    public static HostAndPort resolvePlainRequest(FullHttpRequest req) {
        final String uri=req.uri();
        String hostAndPort;
        if (uri == null || "/".equals(uri) || "".equals(uri)){
            hostAndPort=req.headers().get("Host");
        }else{
            hostAndPort=uri;
            if (hostAndPort.startsWith("http://")){
                hostAndPort=hostAndPort.substring("http://".length());
            }
            if (hostAndPort.contains("/")){
                hostAndPort=hostAndPort.substring(0,hostAndPort.indexOf("/"));
            }
        }
        String port,host;
        if (hostAndPort.contains(":")){
            host=hostAndPort.substring(0,hostAndPort.lastIndexOf(":"));
            port=hostAndPort.substring(hostAndPort.lastIndexOf(":")+1);
        }else{
            host=hostAndPort;
            if ("https://".startsWith(uri)){
                port="80";
            }else {
                port="443";
            }
        }
        return new HostAndPort(host,port);
    }

    public static HostAndPort resolveConnectRequest(FullHttpRequest req) {
        String uri = req.uri();
        if (uri.startsWith("http://")){
            uri=uri.substring("http://".length()).replace("/","");
        }
        String host = uri.substring(0, uri.lastIndexOf(":"));
        String port = uri.substring(uri.lastIndexOf(":") + 1, uri.length());
        return new HostAndPort(host,port);
    }
}
