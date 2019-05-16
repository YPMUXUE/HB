package common.message.frame.establish.translator;


import common.message.MessageTranslator;
import common.message.frame.establish.ConnectionEstablishMessage;
import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;

public class ConnectionEstablishTranslator implements MessageTranslator<ConnectionEstablishMessage> {
	@Override
	public ConnectionEvents getSupportConnectionEvent() {
		return ConnectionEstablishMessage.operationCode;
	}

	@Override
	public ConnectionEstablishMessage translate(ByteBuf buf) {
		short code = buf.readShort();
		if (code != ConnectionEstablishMessage.operationCode.getCode()){
			throw new IllegalArgumentException(code+":code unSupport");
		}
		return new ConnectionEstablishMessage();
	}

	@Override
	public ByteBuf writeByteBuf(ByteBuf buf, ConnectionEstablishMessage message) {
		ConnectionEvents operationCode = message.supportConnectionEvent();
		if (!ConnectionEstablishMessage.operationCode.equals(operationCode)) {
			throw new IllegalArgumentException(operationCode + " ： unSupport code");
		}
		if (!(message instanceof ConnectionEstablishMessage)) {
			throw new IllegalArgumentException(message.getClass().toString());
		}
		ConnectionEstablishMessage m = (ConnectionEstablishMessage) message;
		buf.writeShort(ConnectionEstablishMessage.operationCode.getCode());
		buf.writeInt(0);
		return buf;
	}

	@Override
	public byte[] toBytes(ConnectionEstablishMessage message) {
		throw new UnsupportedOperationException();
	}
}
