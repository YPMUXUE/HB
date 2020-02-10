package priv.common.message.frame.establish.translator;


import priv.common.message.MessageTranslator;
import priv.common.message.frame.establish.ConnectionEstablishFailedMessage;
import priv.common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public class ConnectionEstablishFailedTranslator implements MessageTranslator<ConnectionEstablishFailedMessage> {
	@Override
	public ConnectionEvents getSupportConnectionEvent() {
		return ConnectionEstablishFailedMessage.operationCode;
	}

	@Override
	public ConnectionEstablishFailedMessage translate(ByteBuf buf) {
		short code = buf.readShort();
		if (code != ConnectionEstablishFailedMessage.operationCode.getCode()) {
			throw new IllegalArgumentException(code + ":code unSupport");
		}
		ConnectionEstablishFailedMessage message = new ConnectionEstablishFailedMessage();
		int length = buf.readInt();
		int reasonLength = buf.readShort();
		byte[] reasonEncoded = new byte[reasonLength];
		buf.readBytes(reasonEncoded, 0, reasonLength);
		String reason = new String(reasonEncoded);
		message.setReason(reason);
		return message;
	}

	@Override
	public ByteBuf writeByteBuf(ByteBuf buf, ConnectionEstablishFailedMessage message) {
		ConnectionEvents operationCode = message.supportConnectionEvent();
		if (!ConnectionEstablishFailedMessage.operationCode.equals(operationCode)) {
			throw new IllegalArgumentException(operationCode + " ： unSupport code");
		}
		if (!(message instanceof ConnectionEstablishFailedMessage)) {
			throw new IllegalArgumentException(message.getClass().toString());
		}
		ConnectionEstablishFailedMessage m = (ConnectionEstablishFailedMessage) message;

		String reason = m.getReason();
		byte[] reasonBytes = reason.getBytes(StandardCharsets.UTF_8);
		if (reasonBytes.length > Short.MAX_VALUE) {
			throw new IllegalArgumentException("too long reason " + reasonBytes.length);
		}
		short reasonLength = (short) reasonBytes.length;
		buf.writeShort(ConnectionEstablishFailedMessage.operationCode.getCode());
		buf.writeInt(reasonLength + 2);
		buf.writeShort(reasonLength);
		buf.writeBytes(reasonBytes);
		return buf;
	}

	@Override
	public byte[] toBytes(ConnectionEstablishFailedMessage message) {
		throw new UnsupportedOperationException();
	}
}
