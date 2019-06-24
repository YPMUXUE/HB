package priv.common.resource;

public class SystemConfig {
    public static final int COUNT_OF_PROCESSORS = Runtime.getRuntime().availableProcessors();
    public static final int SUCCESS=1;
    public static final int FAILED=0;

    /**message package config**/
    public static final int IP_ADDRESS_LENGTH=4;
    public static final int IP_PORT_LENGTH=2;
    public static final int DESTINATION_LENGTH=IP_ADDRESS_LENGTH+IP_PORT_LENGTH;
    public static final int HEADER_LENGTH=2;
    public static final int LENGTH_HEADER_LENGTH=4;

	public static final int PACKAGE_MAX_LENGTH = 64 * 1024 * 1024;

}
