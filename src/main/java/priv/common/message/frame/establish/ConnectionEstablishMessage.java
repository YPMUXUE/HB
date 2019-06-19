package priv.common.message.frame.establish;


import priv.common.message.frame.Message;
import priv.common.resource.ConnectionEvents;

public class ConnectionEstablishMessage implements Message {

	public static final ConnectionEvents operationCode = ConnectionEvents.CONNECTION_ESTABLISH;

	@Override
	public ConnectionEvents supportConnectionEvent() {
		return operationCode;
	}
}
