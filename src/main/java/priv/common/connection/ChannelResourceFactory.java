package priv.common.connection;

import io.netty.channel.*;

import java.net.SocketAddress;

public interface ChannelResourceFactory {
	ChannelFuture connect(EventLoop eventLoop, SocketAddress address, ChannelInitializer<Channel> channelInitializer);
	ChannelFuture close(Channel channel, ChannelPromise closePromise);
	ChannelFuture close(Channel channel);
}
