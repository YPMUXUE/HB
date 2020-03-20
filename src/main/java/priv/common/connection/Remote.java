package priv.common.connection;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import priv.Client.bean.HostAndPort;

import java.net.SocketAddress;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Remote<T> {
	public enum Events {
		OP_READ, OP_WRITE, OP_CLOSE;
	}

	T get();

	ChannelFuture write(T data);

	boolean isOpen();
}