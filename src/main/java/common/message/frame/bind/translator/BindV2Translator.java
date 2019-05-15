package common.message.frame.bind.translator;


import common.message.frame.Message;
import common.message.frame.MessageTranslator;
import common.message.frame.bind.BindV2Message;
import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public class BindV2Translator implements MessageTranslator {
	@Override
	public ConnectionEvents getSupportConnectionEvent() {
		return BindV2Message.operationCode;
	}

	@Override
	public Message translate(ByteBuf buf) {
		short code = buf.readShort();
		if (code != BindV2Message.operationCode.getCode()) {
			throw new IllegalArgumentException(code + ":code unSupport");
		}
		if (buf.readableBytes() < 4) {
			throw new IllegalArgumentException("the buf readableBytes is less then 4");
		}
		int length = buf.readInt();
		short hostLength = buf.readShort();
		byte[] hostNameEncoded = new byte[hostLength];
		buf.readBytes(hostNameEncoded, 0, hostLength);
		String hostName = new String(hostNameEncoded, StandardCharsets.UTF_8);
		int port = buf.readShort();
		BindV2Message message = new BindV2Message();
		message.setContentLength(length);
		message.setHostName(hostName);
		message.setPort(port);
		return message;
	}
}
