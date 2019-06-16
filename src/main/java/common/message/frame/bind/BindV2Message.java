package common.message.frame.bind;

import common.message.frame.Message;
import common.resource.ConnectionEvents;

public class BindV2Message implements Message {
	public static final ConnectionEvents operationCode = ConnectionEvents.BIND2;

	private String hostName;
	private int port;
	private int contentLength;

	public BindV2Message(String hostName, int port) {
		this.hostName = hostName;
		this.port = port;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getContentLength() {
		return contentLength;
	}

	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
	}

	@Override
	public ConnectionEvents supportConnectionEvent() {
		return operationCode;
	}

}
