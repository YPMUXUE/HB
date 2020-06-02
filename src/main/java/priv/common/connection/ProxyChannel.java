package priv.common.connection;

import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SocketChannel;

public interface ProxyChannel extends NetworkChannel {
	 int validOps();
	 SocketChannel bind(SocketAddress local);
	 <T> void setOption(SocketOption<T> name, T value);
	shutdownInput();
	shutdownOutput();
	boolean isConnected();
	boolean isBind();
	boolean isConnectionPending();
	boolean connect(SocketAddress remote);
	boolean finishConnect();
	SocketAddress getRemoteAddress();
	read();
	write()

}
