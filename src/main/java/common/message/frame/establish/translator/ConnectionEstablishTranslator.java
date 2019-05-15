package common.message.frame.establish.translator;


import common.message.frame.Message;
import common.message.frame.MessageTranslator;
import common.message.frame.establish.ConnectionEstablishMessage;
import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;

public class ConnectionEstablishTranslator implements MessageTranslator {
	@Override
	public ConnectionEvents getSupportConnectionEvent() {
		return ConnectionEstablishMessage.operationCode;
	}

	@Override
	public Message translate(ByteBuf buf) {
		short code = buf.readShort();
		if (code != ConnectionEstablishMessage.operationCode.getCode()){
			throw new IllegalArgumentException(code+":code unSupport");
		}
		return new ConnectionEstablishMessage();
	}
}
