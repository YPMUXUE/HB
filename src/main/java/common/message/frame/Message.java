package common.message.frame;

import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;


public interface Message {
	byte[] toBytes();

	ByteBuf writeByteBuf(ByteBuf byteBuf);

	ConnectionEvents getSupportConnectionEvent();

	void load(byte[] bytes);

	void load(ByteBuf byteBuf);

	int size();
}
