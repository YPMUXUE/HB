package priv.Client.handler2;

import io.netty.util.AttributeKey;
import priv.Client.bean.HostAndPort;

/**
 *  * @author  pyuan
 *  * @date    2019/8/29 0029
 *  * @Description
 *  *
 *  
 */
public class ConfigAttributeKey {
	public static final AttributeKey<HostAndPort> HOST_AND_PORT_ATTRIBUTE_KEY = AttributeKey.newInstance("HostAndPort");

}
