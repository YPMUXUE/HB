package priv.common.message.frame.close.translator;

import priv.common.message.MessageTranslator;
import priv.common.message.frame.close.CloseMessage;
import priv.common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;

public class CloseTranslator implements MessageTranslator<CloseMessage> {
	@Override
	public ConnectionEvents getSupportConnectionEvent() {
		return CloseMessage.operationCode;
	}

	@Override
	public CloseMessage translate(ByteBuf buf) {
		short code = buf.readShort();
		if (code != CloseMessage.operationCode.getCode()) {
			throw new IllegalArgumentException(code + ":code unSupport");
		}
		return new CloseMessage();
	}

	@Override
	public ByteBuf writeByteBuf(ByteBuf buf, CloseMessage message) {
		buf.writeShort(CloseMessage.operationCode.getCode());
		buf.writeInt(0);
		return buf;
	}

	@Override
	public byte[] toBytes(CloseMessage message) {
		throw new UnsupportedOperationException();
	}
}
