package priv.Server.handler2;

import io.netty.buffer.ByteBuf;
import priv.common.message.frame.Message;

import java.net.InetSocketAddress;

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
	void write(Message data);
	void bind(Message bindMessage);
	void close();
	InetSocketAddress getRemoteAddress();

}
