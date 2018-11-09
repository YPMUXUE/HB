package Client.bean;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HostAndPort {
    private String host;
    private String port;

    public HostAndPort(String host, String port) {
        this.host = host;
        this.port = port;
    }

    public InetAddress getHost()throws UnknownHostException {
        return InetAddress.getByName(this.host);
    }
    public int getPort(){
        return Integer.valueOf(port);
    }
}
