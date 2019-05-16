package common.message.frame.bind.translator;


import common.message.MessageTranslator;
import common.message.frame.bind.BindV1Message;
import common.resource.ConnectionEvents;
import common.resource.SystemConfig;
import io.netty.buffer.ByteBuf;

public class BindV1Translator implements MessageTranslator<BindV1Message> {

	@Override
	public ConnectionEvents getSupportConnectionEvent() {
		return BindV1Message.operationCode;
	}

	@Override
	public BindV1Message translate(ByteBuf buf) {
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

	@Override
	public ByteBuf writeByteBuf(ByteBuf buf, BindV1Message message) {
		ConnectionEvents operationCode = message.supportConnectionEvent();
		if (!BindV1Message.operationCode.equals(operationCode)) {
			throw new IllegalArgumentException(operationCode + " ： unSupport code");
		}
		if (!(message instanceof BindV1Message)) {
			throw new IllegalArgumentException(message.getClass().toString());
		}
		BindV1Message m = (BindV1Message) message;
		byte[] des = m.getDestination();
		if (des.length != SystemConfig.DESTINATION_LENGTH){
			throw new IllegalArgumentException("wrong length of destination ：" + des.length);
		}
		int contentLength = des.length;
		buf.writeShort(m.supportConnectionEvent().getCode());
		buf.writeInt(contentLength);
		buf.writeBytes(des,0,des.length);
		return buf;
	}

	@Override
	public byte[] toBytes(BindV1Message message) {
		throw new UnsupportedOperationException();
	}
}
