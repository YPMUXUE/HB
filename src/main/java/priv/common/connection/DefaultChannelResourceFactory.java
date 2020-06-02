package priv.common.connection;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class DefaultChannelResourceFactory implements ChannelResourceFactory {


	private final Map<ChannelOption<Object>, Object> options;

	public <T> DefaultChannelResourceFactory(Map<ChannelOption,Object> options) {
		this.options = Collections.unmodifiableMap(options);
	}

	@Override
	public ChannelFuture connect(EventLoop eventLoop, SocketAddress address, ChannelInitializer<Channel> channelInitializer){
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(Objects.requireNonNull(eventLoop))
				.channel(NioSocketChannel.class)
				.handler(channelInitializer);
		for (Map.Entry<ChannelOption<Object>, Object> entry : options.entrySet()) {
			ChannelOption<Object> key = entry.getKey();
			Object value = entry.getValue();
			bootstrap.option(key, value);
		}
		return bootstrap.connect(address);
	}

	@Override
	public ChannelFuture close(Channel channel, ChannelPromise closePromise) {
		channel.close(closePromise);
		return closePromise;
	}

	@Override
	public ChannelFuture close(Channel channel) {
		return channel.close();
	}
}
