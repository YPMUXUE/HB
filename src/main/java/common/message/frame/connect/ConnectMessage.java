package common.message.frame.connect;


import common.message.frame.AbstractByteBufContentMessage;
import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;

public class ConnectMessage extends AbstractByteBufContentMessage {
	private int inputLength;

	public static final ConnectionEvents operationCode = ConnectionEvents.CONNECT;

	public ConnectMessage() {
	}

	public ConnectMessage(ByteBuf content) {
		this.content = content;
	}

	public void setInputLength(int inputLength) {
		this.inputLength = inputLength;
	}

	public int getInputLength() {
		return inputLength;
	}

	@Override
	public ConnectionEvents supportConnectionEvent() {
		return operationCode;
	}

	@Deprecated
	public void load(ByteBuf byteBuf) {
		short operationCode = byteBuf.readShort();
		if (operationCode != ConnectMessage.operationCode.getCode()) {
			throw new IllegalArgumentException(operationCode + "");
		}
		this.inputLength = byteBuf.readInt();
		this.content = byteBuf.retain();
	}
}
