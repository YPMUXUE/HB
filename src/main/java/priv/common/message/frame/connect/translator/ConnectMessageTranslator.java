package priv.common.message.frame.connect.translator;


import priv.common.message.frame.connect.ConnectMessage;
import priv.common.message.MessageTranslator;
import priv.common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;

public class ConnectMessageTranslator implements MessageTranslator<ConnectMessage> {
	@Override
	public ConnectionEvents getSupportConnectionEvent() {
		return ConnectMessage.operationCode;
	}

	@Override
	public ConnectMessage translate(ByteBuf buf) {
		short operationCode = buf.readShort();
		if (operationCode != ConnectMessage.operationCode.getCode()){
			throw new IllegalArgumentException(operationCode+"");
		}
		ConnectMessage message = new ConnectMessage();
//		message.load(buf);

		message.setInputLength(buf.readInt());
		message.setContent(buf.retain());
		return message;
	}

	@Override
	public ByteBuf writeByteBuf(ByteBuf buf, ConnectMessage message) {
		ConnectionEvents operationCode = message.supportConnectionEvent();
		if (!ConnectMessage.operationCode.equals(operationCode)) {
			throw new IllegalArgumentException(operationCode + " ： unSupport code");
		}
		if (!(message instanceof ConnectMessage)) {
			throw new IllegalArgumentException(message.getClass().toString());
		}
		ConnectMessage m = (ConnectMessage) message;
		buf.writeShort(m.supportConnectionEvent().getCode());
		buf.writeInt(m.getContent().readableBytes());
		buf.writeBytes(m.getContent());
		return buf;
	}

	@Override
	public byte[] toBytes(ConnectMessage message) {
		throw new UnsupportedOperationException();
	}
}
