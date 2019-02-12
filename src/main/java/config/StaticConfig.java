package config;

import java.net.InetAddress;

public class StaticConfig {
    /**message package config**/
    public static final int IP_ADDRESS_LENGTH=4;
    public static final int IP_PORT_LENGTH=2;
    public static final int DESTINATION_LENGTH=IP_ADDRESS_LENGTH+IP_PORT_LENGTH;
    public static final int HEADER_LENGTH=2;
    public static final int LENGTH_HEADER_LENGTH=4;


    /**address config**/
    public static final String PROXY_SERVER_ADDRESS = "127.0.0.1";
    public static final int PROXY_SERVER_PORT = 5559;


}
