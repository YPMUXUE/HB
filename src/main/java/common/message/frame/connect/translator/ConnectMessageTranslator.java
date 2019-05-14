package common.message.frame.connect.translator;


import common.message.frame.Message;
import common.message.frame.connect.ConnectMessage;
import common.message.frame.MessageTranslator;
import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;

public class ConnectMessageTranslator implements MessageTranslator {
	@Override
	public ConnectionEvents getSupportConnectionEvent() {
		return ConnectMessage.operationCode;
	}

	@Override
	public Message translate(ByteBuf buf) {
		short code = buf.getShort(0);
		if (code != ConnectMessage.operationCode.getCode()){
			throw new IllegalArgumentException(code+"");
		}
		ConnectMessage message = new ConnectMessage();
		message.load(buf);
		return message;
	}
}
