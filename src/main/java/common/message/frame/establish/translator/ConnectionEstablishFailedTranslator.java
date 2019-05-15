package common.message.frame.establish.translator;


import common.message.frame.Message;
import common.message.frame.MessageTranslator;
import common.message.frame.establish.ConnectionEstablishFailedMessage;
import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;

public class ConnectionEstablishFailedTranslator implements MessageTranslator {
	@Override
	public ConnectionEvents getSupportConnectionEvent() {
		return ConnectionEstablishFailedMessage.operationCode;
	}

	@Override
	public Message translate(ByteBuf buf) {
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
}
