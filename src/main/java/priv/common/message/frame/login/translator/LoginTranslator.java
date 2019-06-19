package priv.common.message.frame.login.translator;


import priv.common.message.MessageTranslator;
import priv.common.message.frame.login.LoginMessage;
import priv.common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;

public class LoginTranslator implements MessageTranslator<LoginMessage> {
	@Override
	public ConnectionEvents getSupportConnectionEvent() {
		return LoginMessage.operationCode;
	}

	@Override
	public LoginMessage translate(ByteBuf buf) {
		short code = buf.readShort();
		if (code != LoginMessage.operationCode.getCode()) {
			throw new IllegalArgumentException(code + ":code unSupport");
		}
		LoginMessage message = new LoginMessage();
		int length = buf.readInt();
		int pwdLength = buf.readShort();
		byte[] reasonEncoded = new byte[pwdLength];
		buf.readBytes(reasonEncoded, 0, pwdLength);
		message.setPwd(reasonEncoded);
		return message;
	}

	@Override
	public ByteBuf writeByteBuf(ByteBuf buf, LoginMessage message) {
		ConnectionEvents operationCode = message.supportConnectionEvent();
		if (!LoginMessage.operationCode.equals(operationCode)) {
			throw new IllegalArgumentException(operationCode + " ： unSupport code");
		}
		if (!(message instanceof LoginMessage)) {
			throw new IllegalArgumentException(message.getClass().toString());
		}
		LoginMessage m = (LoginMessage) message;

		if (m.getPwd().length > Short.MAX_VALUE) {
			throw new IllegalArgumentException("too long pwd" + m.getPwd().length);
		}
		short pwdLength = (short) m.getPwd().length;
		buf.writeShort(LoginMessage.operationCode.getCode());
		buf.writeInt(pwdLength + 2);
		buf.writeShort(pwdLength);
		buf.writeBytes(m.getPwd());
		return buf;
	}

	@Override
	public byte[] toBytes(LoginMessage message) {
		throw new UnsupportedOperationException();
	}
}
