package priv.common.message.frame.login;

import priv.common.message.frame.Message;
import priv.common.resource.ConnectionEvents;


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
