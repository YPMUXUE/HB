package priv.Server.handler2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import priv.common.message.frame.Message;
import priv.common.message.frame.bind.BindV1Message;
import priv.common.message.frame.bind.BindV2Message;

import java.net.InetSocketAddress;

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
	public void write(Message data) {

	}

	@Override
	public void bind(Message bindMessage) {
		if (bindMessage instanceof BindV1Message){
			handleBindV1((BindV1Message)bindMessage);
		}else if (bindMessage instanceof BindV2Message){
			handleBindV2((BindV2Message)bindMessage);
		}else{
			throw new IllegalArgumentException("wrong type of bindMessage"+bindMessage);
		}
	}

	@Override
	public void close() {

	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return null;
	}

	private void handleBindV1(BindV1Message bindMessage) {

	}

	private void handleBindV2(BindV2Message bindMessage) {
	}
}
