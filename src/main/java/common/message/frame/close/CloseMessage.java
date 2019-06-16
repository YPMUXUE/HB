package common.message.frame.close;

import common.message.frame.Message;
import common.resource.ConnectionEvents;

public class CloseMessage implements Message {
	public static final ConnectionEvents operationCode = ConnectionEvents.CLOSE;

	@Override
	public ConnectionEvents supportConnectionEvent() {
		return operationCode;
	}
}
