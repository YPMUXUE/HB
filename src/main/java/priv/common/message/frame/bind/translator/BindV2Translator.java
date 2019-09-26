package priv.common.message.frame.bind.translator;


import priv.common.message.MessageTranslator;
import priv.common.message.frame.bind.BindV2Message;
import priv.common.resource.ConnectionEvents;
import priv.common.resource.SystemConfig;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public class BindV2Translator implements MessageTranslator<BindV2Message> {
	@Override
	public ConnectionEvents getSupportConnectionEvent() {
		return BindV2Message.operationCode;
	}

	@Override
	public BindV2Message translate(ByteBuf buf) {
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
		int port = buf.readUnsignedShort();
		BindV2Message message = new BindV2Message(hostName,port);
		message.setContentLength(length);
		return message;
	}

	@Override
	public ByteBuf writeByteBuf(ByteBuf buf, BindV2Message message) {
		ConnectionEvents operationCode = message.supportConnectionEvent();
		if (!BindV2Message.operationCode.equals(operationCode)) {
			throw new IllegalArgumentException(operationCode + " ： unSupport code");
		}
		if (!(message instanceof BindV2Message)) {
			throw new IllegalArgumentException(message.getClass().toString());
		}
		BindV2Message m = (BindV2Message) message;
		String host = m.getHostName();
		byte[] hostBytes = host.getBytes(StandardCharsets.UTF_8);
		if (hostBytes.length > Short.MAX_VALUE){
			throw new IllegalArgumentException("too long host:"+host);
		}
		short hostLength = (short) hostBytes.length;
		int contentLength = 2 + hostLength + SystemConfig.IP_PORT_LENGTH;
		buf.writeShort(BindV2Message.operationCode.getCode());
		buf.writeInt(contentLength);
		buf.writeShort(hostLength);
		buf.writeBytes(hostBytes, 0 ,hostLength);
		buf.writeShort(m.getPort());
		return buf;

	}


	@Override
	public byte[] toBytes(BindV2Message message) {
		throw new UnsupportedOperationException();
	}
}
