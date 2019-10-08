package priv.Server.handler2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import priv.Client.bean.HostAndPort;
import priv.common.message.frame.bind.BindV1Message;
import priv.common.message.frame.bind.BindV2Message;

/**
 *  * @author  pyuan
 *  * @date    2019/9/27 0027
 *  * @Description
 *  *
 *  
 */
public class DefaultRemoteConnection implements RemoteConnection {
	private final Channel callBackChannel;
	private ChannelFuture targetChannelFuture;
	public DefaultRemoteConnection(Channel callBackChannel) {
		this.callBackChannel = callBackChannel;
	}

	@Override
	public ByteBuf read() {
		return null;
	}

	@Override
	public void write(ByteBuf data) {

	}

	@Override
	public void bind(HostAndPort hostAndPort) {

	}

	@Override
	public void close() {

	}

	@Override
	public Channel getChannel() {
		return null;
	}

	private void handleBindV1(BindV1Message bindMessage) {

	}

	private void handleBindV2(BindV2Message bindMessage) {
	}
}
