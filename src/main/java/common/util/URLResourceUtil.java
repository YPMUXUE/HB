package common.util;

import common.resource.SystemConfig;

import java.net.URL;

public class URLResourceUtil {
    public static String getCurrentClassPath(){
        URL resource= SystemConfig.class.getClassLoader().getResource("io/netty/buffer/ByteBuf.class");
        if (resource.getProtocol().equalsIgnoreCase("jar")){
            String url=resource.getPath().substring(resource.getPath().indexOf(":")+2);
            url=url.substring(0,url.indexOf(".jar!/")+4);
        }
return null;
    }

    public static void main(String[] args) {
        getCurrentClassPath();
    }
}
