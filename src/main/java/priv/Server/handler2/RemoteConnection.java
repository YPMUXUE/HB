package priv.Server.handler2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import priv.Client.bean.HostAndPort;

/**
 *  * @author  pyuan
 *  * @date    2019/9/27 0027
 *  * @Description
 *  *
 *  
 */
public interface RemoteConnection {
	ByteBuf read();
	void write(ByteBuf data);
	void bind(HostAndPort hostAndPort);
	void close();
	Channel getChannel();

}
