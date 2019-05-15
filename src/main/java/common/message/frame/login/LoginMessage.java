package common.message.frame.login;

import common.message.frame.Message;
import common.resource.ConnectionEvents;


public class LoginMessage implements Message {
	public static final ConnectionEvents operationCode = ConnectionEvents.LOGIN;
	private byte[] pwd;
	@Override
	public ConnectionEvents supportConnectionEvent() {
		return operationCode;
	}

	public byte[] getPwd() {
		return pwd;
	}

	public void setPwd(byte[] pwd) {
		this.pwd = pwd;
	}
}
