package priv.common.util;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.SocketAddress;
import java.util.Objects;


public class Connections {
	private static final Bootstrap defaultBootstrap=new Bootstrap();
//	public static final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

	public static ChannelFuture connect(EventLoop eventLoop, SocketAddress address, ChannelInitializer channelInitializer){
		Bootstrap bootstrap = defaultBootstrap.clone();
		bootstrap.group(Objects.requireNonNull(eventLoop))
				.channel(NioSocketChannel.class)
				.handler(channelInitializer);
		return bootstrap.connect(address);
	}
}
