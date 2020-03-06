package priv.common.connection;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.ByteToMessageDecoder;
import priv.Client.bean.HostAndPort;
import priv.common.message.frame.Message;
import priv.common.message.frame.close.CloseMessage;
import priv.common.message.frame.connect.ConnectMessage;
import priv.common.message.frame.establish.ConnectionEstablishFailedMessage;
import priv.common.message.frame.establish.ConnectionEstablishMessage;
import priv.common.resource.ConnectionEvents;

import java.net.SocketAddress;
import java.util.function.Consumer;

/**
 *  * @author  pyuan
 *  * @date    2020/3/6 0006
 *  * @Description
 *  *
 *  
 */
public class ProxyRemoteConnection implements Remote<ByteBuf>,RemoteDataHandler<Message> {
	public static final ByteToMessageDecoder.Cumulator MERGE_CUMULATOR = ByteToMessageDecoder.MERGE_CUMULATOR;
	public static final ByteToMessageDecoder.Cumulator COMPOSITE_CUMULATOR = ByteToMessageDecoder.COMPOSITE_CUMULATOR;

	private  Consumer<Events> listener;
	private ByteBuf buffer;
	private final int capacityFlag;
	private ChannelFuture bindFuture;

	public ProxyRemoteConnection(int capacityFlag) {
		this.capacityFlag = capacityFlag;
	}

	public ProxyRemoteConnection() {
		this(10*1024*1024);
	}

	@Override
	public ByteBuf get() {
		ByteBuf result;
		synchronized (this){
			result = this.buffer;
			this.buffer = null;
		}
		return result;
	}

	@Override
	public ChannelFuture write(ByteBuf data) {
this.bindFuture.channel().writeAndFlush();
	}

	public void writeBuffer(ByteBuf data) {
		synchronized (this){
			ByteBuf cumulation  = this.buffer;
			if (cumulation == null){
				cumulation = data;
			}else{
				ByteBufAllocator alloc = cumulation.alloc();
				cumulation = MERGE_CUMULATOR.cumulate(alloc, cumulation, data);
			}
			this.buffer = cumulation;
		}
		listener.accept(Events.OP_READ);

	}

	@Override
	public boolean isOpen() {
		return false;
	}

	@Override
	public ChannelFuture close() {
		return null;
	}

	@Override
	public ChannelFuture connect(HostAndPort hostAndPort) {
		return null;
	}

	@Override
	public ChannelFuture connect(SocketAddress address) {
		return null;
	}

	@Override
	public void receiveData(Message data) {
		ConnectionEvents events = data.supportConnectionEvent();
		switch (events){
			case CONNECT:
				this.writeBuffer(((ConnectMessage)data).getContent());
				break;
			case CLOSE:
				this.onReceiveCloseMessage((CloseMessage)data);
				break;
			case CONNECTION_ESTABLISH_FAILED:
				this.onReceiveConnectionEstablishFailed((ConnectionEstablishFailedMessage)data);
				break;
			case CONNECTION_ESTABLISH:
				this.onReceiveConnectionEstablish((ConnectionEstablishMessage)data);
				break;
			default:
				throw new IllegalArgumentException("unknown events"+data.toString());
		}
	}

	private void onReceiveConnectionEstablish(ConnectionEstablishMessage data) {
	}

	private void onReceiveConnectionEstablishFailed(ConnectionEstablishFailedMessage data) {
	}

	private void onReceiveCloseMessage(CloseMessage data) {

	}
}
