import Client.bean.HostAndPort;

public class MainTest {
    public static void main(String[] args) {
        String uri="bbs.nga.cn";
        String hostAndPort;
//        if (uri == null || "/".equals(uri) || "".equals(uri)){
//            hostAndPort=req.headers().get("Host");
//        }else{
            hostAndPort=uri;
            if (hostAndPort.startsWith("http://")){
                hostAndPort=uri.substring("http://".length());
            }
            if (hostAndPort.contains("/")){
                hostAndPort=hostAndPort.substring(0,hostAndPort.indexOf("/"));
            }
//        }
        String port,host;
        if (hostAndPort.contains(":")){
            host=hostAndPort.substring(0,hostAndPort.lastIndexOf(":"));
            port=hostAndPort.substring(hostAndPort.lastIndexOf(":")+1);
        }else{
            host=hostAndPort;
            port="80";
        }
    }
}
