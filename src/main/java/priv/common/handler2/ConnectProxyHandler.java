package priv.common.handler2;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 *  * @author  pyuan
 *  * @date    2019/8/26 0026
 *  * @Description
 *  *
 *  
 */
public class ConnectProxyHandler extends ChannelInboundHandlerAdapter {
	private final Channel targetChannel;

	public ConnectProxyHandler(Channel targetChannel) {
		this.targetChannel = targetChannel;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		targetChannel.writeAndFlush(msg);
	}
}
