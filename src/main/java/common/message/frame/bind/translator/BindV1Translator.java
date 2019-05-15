package common.message.frame.bind.translator;


import common.message.frame.Message;
import common.message.frame.MessageTranslator;
import common.message.frame.bind.BindV1Message;
import common.resource.ConnectionEvents;
import common.resource.SystemConfig;
import io.netty.buffer.ByteBuf;

public class BindV1Translator implements MessageTranslator {

	@Override
	public ConnectionEvents getSupportConnectionEvent() {
		return BindV1Message.operationCode;
	}

	@Override
	public Message translate(ByteBuf buf) {
		short code = buf.readShort();
		if (code != BindV1Message.operationCode.getCode()) {
			throw new IllegalArgumentException(code + ":code unSupport");
		}
		if (buf.readableBytes() < 4) {
			throw new IllegalArgumentException("the buf readableBytes is less then 4");
		}
		int length = buf.readInt();

		if (buf.readableBytes() < SystemConfig.DESTINATION_LENGTH) {
			throw new IllegalArgumentException("the buf readableBytes is less then " + SystemConfig.DESTINATION_LENGTH);
		}
		BindV1Message message = new BindV1Message();
		message.setContentLength(length);
		byte[] des = new byte[SystemConfig.DESTINATION_LENGTH];
		buf.readBytes(des, 0, SystemConfig.DESTINATION_LENGTH);
		message.setDestination(des);
		return message;
	}
}
