package common.message.frame;

import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;


public interface Message {
	ConnectionEvents supportConnectionEvent();

}
