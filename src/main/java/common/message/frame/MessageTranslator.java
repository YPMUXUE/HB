package common.message.frame;


import common.message.frame.Message;
import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;

public interface MessageTranslator {
	ConnectionEvents getSupportConnectionEvent();

	Message translate(ByteBuf buf);
}
