package common.message.frame.bind;


import common.message.frame.Message;
import common.resource.ConnectionEvents;

public class BindV1Message implements Message {
	public static final ConnectionEvents operationCode = ConnectionEvents.BIND;

	private byte[] destination;
	private int contentLength;

	public byte[] getDestination() {
		return destination;
	}

	public void setDestination(byte[] destination) {
		this.destination = destination;
	}

	public int getContentLength() {
		return contentLength;
	}

	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
	}

	@Override
	public ConnectionEvents supportConnectionEvent() {
		return BindV1Message.operationCode;
	}
}
