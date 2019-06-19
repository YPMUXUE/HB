package priv.Client.bean;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HostAndPort {
    private String host;
    private String port;
    public static HostAndPort resolve(FullHttpRequest req){
        if (HttpMethod.CONNECT.equals(req.method())){
            return resolve(req.uri());
        }else {
            return resolve(req.uri(),req.headers().get("Host"));
        }
    }

    public static HostAndPort resolve(String uri) {
        HostAndPort result=new HostAndPort();
        result.host = uri.substring(0, uri.lastIndexOf(":"));
        result.port = uri.substring(uri.lastIndexOf(":") + 1);
        return result;
    }

    public static HostAndPort resolve(String uri, String host) {
        HostAndPort result=new HostAndPort();
        if (!uri.startsWith("/")) {
            String protocol = uri.substring(0, uri.indexOf("://"));
            String hostAndPort = uri.substring(uri.indexOf("://") + 3);
            if (hostAndPort.contains("/")) {
                hostAndPort = hostAndPort.substring(0, hostAndPort.indexOf("/"));
            }
            if (hostAndPort.contains(":")) {
                result.port = hostAndPort.substring(hostAndPort.indexOf(":") + 1);
                result.host = hostAndPort.substring(0, hostAndPort.indexOf(":"));
            } else {
                result.port = Protocol.valueOf(protocol).getPort();
                result.host = hostAndPort;
            }
        }else{
            if (host.contains(":")) {
                result.port = host.substring(host.indexOf(":") + 1);
                result.host = host.substring(0, host.indexOf(":"));
            } else {
                result.port = "80";
                result.host = host;
            }
        }
        return result;
    }

    @Deprecated
    private static HostAndPort resolvePlainRequest(FullHttpRequest req) {
        HostAndPort result=new HostAndPort();
        final String uri=req.uri();
        if (!uri.startsWith("/")) {
            String protocol = uri.substring(0, uri.indexOf("://"));
            String hostAndPort = uri.substring(uri.indexOf("://") + 3);
            if (hostAndPort.contains("/")) {
                hostAndPort = hostAndPort.substring(0, hostAndPort.indexOf("/"));
            }
            if (hostAndPort.contains(":")) {
                result.port = hostAndPort.substring(hostAndPort.indexOf(":") + 1);
                result.host = hostAndPort.substring(0, hostAndPort.indexOf(":"));
            } else {
                result.port = Protocol.valueOf(protocol).getPort();
                result.host = hostAndPort;
            }
        }else{
          final String hostAndPort=req.headers().get("Host");
            if (hostAndPort.contains(":")) {
                result.port = hostAndPort.substring(hostAndPort.indexOf(":") + 1);
                result.host = hostAndPort.substring(0, hostAndPort.indexOf(":"));
            } else {
                result.port = "80";
                result.host = hostAndPort;
            }
        }
            return result;
    }

    @Deprecated
    private static HostAndPort resolveConnectRequest(FullHttpRequest req) {
        HostAndPort result=new HostAndPort();
        final String uri = req.uri();
        result.host = uri.substring(0, uri.lastIndexOf(":"));
        result.port = uri.substring(uri.lastIndexOf(":") + 1);
        return result;
    }

    private HostAndPort(){}

    public HostAndPort(String host, String port) {
        this.host = host;
        this.port = port;
    }

    public InetAddress getHost()throws UnknownHostException {
        return InetAddress.getByName(this.host);
    }
    public String getHostString(){
        return host;
    }
    public int getPort(){
        return Integer.valueOf(port);
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)){
            return true;
        }
        if (!(obj instanceof HostAndPort)){
            return false;
        }
        HostAndPort o = (HostAndPort) obj;
        return this.host.equals(o.host) && this.port.equals(o.port);
    }

    @Override
    public int hashCode() {
        return this.host.hashCode()^this.port.hashCode();
    }
}
