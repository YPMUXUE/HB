package priv.common.message.frame.close;

import priv.common.message.frame.Message;
import priv.common.resource.ConnectionEvents;

public class CloseMessage implements Message {
	public static final ConnectionEvents operationCode = ConnectionEvents.CLOSE;

	@Override
	public ConnectionEvents supportConnectionEvent() {
		return operationCode;
	}
}
