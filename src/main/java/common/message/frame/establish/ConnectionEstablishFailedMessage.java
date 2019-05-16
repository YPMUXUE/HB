package common.message.frame.establish;


import common.message.frame.Message;
import common.resource.ConnectionEvents;

public class ConnectionEstablishFailedMessage implements Message {

	public static final ConnectionEvents operationCode = ConnectionEvents.CONNECTION_ESTABLISH_FAILED;
	private String reason;

	public ConnectionEstablishFailedMessage() {
	}

	public ConnectionEstablishFailedMessage(String reason) {
		this.reason = reason;
	}

	@Override
	public ConnectionEvents supportConnectionEvent() {
		return operationCode;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
