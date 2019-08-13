package priv.Client.bean;

import io.netty.handler.codec.http.FullHttpRequest;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HostAndPort {
    private String host;
    private String port;
    private String protocol;
    public static HostAndPort resolve(FullHttpRequest req){
        return resolveNew(req.uri(),req.headers().get("Host"));
    }

    public static HostAndPort resolveNew(String uri, String hostHeader) {
        HostAndPort result = new HostAndPort();

        ///////////////uri
        if (!uri.startsWith("/")) {
            int i;
            //http://www.example.com:8080/x?x=1 =======> www.example.com:8080/x?x=1
            if ((i = uri.indexOf("://")) >= 0) {
                String protocol = uri.substring(0, i);
                result.port = Protocol.valueOf(protocol).getPort();
                uri = uri.substring(i + 3);
            }
            // www.example.com:8080/x?x:=1 ======> www.example.com:8080
            if ((i = uri.indexOf("/")) > 0) {
                uri = uri.substring(0, i);
            }
            //www.example.com:8080 =======> www.example.com
            if ((i = uri.indexOf(":")) > 0) {
                result.port = uri.substring(i + 1);
                result.host = uri.substring(0, i);
            }
        }


        /////hostHeader
        if (StringUtils.isNotEmpty(hostHeader)) {
            int i;
            //http://www.example.com:8080/x?x=1 =======> www.example.com:8080/x?x=1
            if ((i = hostHeader.indexOf("://")) >= 0) {
                result.protocol = hostHeader.substring(0, i);
                hostHeader = hostHeader.substring(i + 3);
                result.port = Protocol.valueOf(result.protocol).getPort();
            }

            // www.example.com:8080/x?x:=1 ======> www.example.com:8080
            if ((i = hostHeader.indexOf("/")) > 0) {
                hostHeader = hostHeader.substring(0, i);
            }

            //www.example.com:8080 =======> www.example.com
            if ((i = hostHeader.indexOf(":")) >= 0) {
                result.port = hostHeader.substring(i + 1);
                result.host = hostHeader.substring(0, i);
            }
        }

        if (StringUtils.isEmpty(result.host) || StringUtils.isEmpty(result.port)) {
            throw new NullPointerException("Host or Port can not be null");
        }
        return result;
    }

//    private static void resolve(String str , HostAndPort hostAndPort){
//        int i;
//        // http://www.example.com:8080/x?x=1
//        if ((i = str.indexOf("://")) >= 0) {
//            String protocol = str.substring(0, i);
//            hostAndPort.port = Protocol.valueOf(protocol).getPort();
//            str = str.substring(i + 3);
//        }
//        // www.example.com:8080/x?x=1
//        if (str.startsWith("/")){
//            str = str.substring(1);
//        }
//        // www.example.com:8080/x?x=1
//        if ((i = str.indexOf("/"))>0){
//            str = str.substring(0,i);
//        }
//        // www.example.com:8080
//        if ((i = str.indexOf(":"))>0) {
//            hostAndPort.port = str.substring(i + 1);
//            str = str.substring(0, i);
//        }
//        // www.example.com
//        hostAndPort.host = str;
//
//
//    }

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

    public static void main(String[] args) {
//        String url = "http://www.example.com:8080/test?t=1";
//        String host = "http://www.example.com";
        String url = "103.254.188.50:80";
        String host = "103.254.188.50:80";
        HostAndPort resolve = resolveNew(url, host);
        System.out.println(resolve.host+"----"+resolve.port);
    }
}
