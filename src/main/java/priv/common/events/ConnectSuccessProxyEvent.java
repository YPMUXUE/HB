package priv.common.events;

import io.netty.channel.Channel;

/**
 *  * @author  pyuan
 *  * @date    2019/8/27 0027
 *  * @Description
 *  *
 *  
 */
public class ConnectSuccessProxyEvent implements ProxyEvent {
	private final Channel channel;

	public ConnectSuccessProxyEvent(Channel channel) {
		this.channel = channel;
	}

	public Channel getChannel() {
		return channel;
	}
}
