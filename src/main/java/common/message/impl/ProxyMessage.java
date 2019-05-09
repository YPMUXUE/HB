package common.message.impl;


import common.message.AbstractByteBufContentMessage;
import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;

public class ProxyMessage extends AbstractByteBufContentMessage {

	private static final ConnectionEvents operationCode = ConnectionEvents.CONNECT;

	@Override
	public byte[] toBytes() {
		throw new UnsupportedOperationException();
	}


	@Override
	public ByteBuf writeByteBuf(ByteBuf byteBuf) {
		return null;
	}

	@Override
	public ConnectionEvents handleType() {
		return operationCode;
	}

	@Override
	public void load(byte[] bytes) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void load(ByteBuf byteBuf) {

	}

	@Override
	public int size() {
		return 0;
	}
}
