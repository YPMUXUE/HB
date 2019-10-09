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
        return resolve(req.uri(),req.headers().get("Host"));
    }

    public static HostAndPort resolve(String uri, String hostHeader) {
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
            }else{
                result.host = uri;
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
            }else{
                result.host = hostHeader;
            }
        }

        if (StringUtils.isEmpty(result.host) || StringUtils.isEmpty(result.port)) {
            throw new NullPointerException("Host or Port can not be null,uri:"+uri+",header:"+hostHeader);
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
        return Integer.parseInt(port);
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

    @Override
    public String toString() {
        return super.toString() + "[" + host + ":" + port + "]";
    }

    public static void main(String[] args) {
//        String url = "http://www.example.com:8080/test?t=1";
//        String host = "http://www.example.com";
        String url = "profile.firefox.com.cn:443";
        String host = "profile.firefox.com.cn:443";
        HostAndPort resolve = resolve(url, host);
        System.out.println(resolve.host+"----"+resolve.port);
    }
}
