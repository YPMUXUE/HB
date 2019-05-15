package common.message.frame;


import common.message.frame.Message;
import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;

public interface MessageTranslator {
	ConnectionEvents getSupportConnectionEvent();

	Message translate(ByteBuf buf);

	ByteBuf writeByteBuf(ByteBuf buf, Message message);

	byte[] toBytes(Message message);
}
