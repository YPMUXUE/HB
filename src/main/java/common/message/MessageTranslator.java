package common.message;



import common.message.frame.Message;
import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;

public interface MessageTranslator<T extends Message> {
	ConnectionEvents getSupportConnectionEvent();

	T translate(ByteBuf buf);

	ByteBuf writeByteBuf(ByteBuf buf, T message);

	byte[] toBytes(T message);
}
