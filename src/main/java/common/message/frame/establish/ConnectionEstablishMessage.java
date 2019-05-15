package common.message.frame.establish;


import common.message.frame.Message;
import common.resource.ConnectionEvents;

public class ConnectionEstablishMessage implements Message {

	public static final ConnectionEvents operationCode = ConnectionEvents.CONNECTION_ESTABLISH;

	@Override
	public ConnectionEvents supportConnectionEvent() {
		return operationCode;
	}
}
