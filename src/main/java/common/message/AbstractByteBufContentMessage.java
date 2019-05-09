package common.message;


import common.resource.ConnectionEvents;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;

public abstract class AbstractByteBufContentMessage implements ByteBufContentMessage {
	protected ByteBuf content;

//	@Override
//	public byte[] toBytes() {
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	public ByteBuf toByteBuf() {
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	public ByteBuf writeByteBuf(ByteBuf byteBuf) {
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	public ConnectionEvents handleType() {
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	public void load(byte[] bytes) {
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	public void load(ByteBuf byteBuf) {
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	public int size() {
//		throw new UnsupportedOperationException();
//	}

	@Override
	public ByteBuf getContent() {
		return content;
	}

	@Override
	public int refCnt() {
		return content.refCnt();
	}

	@Override
	public ReferenceCounted retain() {
		content.retain();
		return this;
	}

	@Override
	public ReferenceCounted retain(int increment) {
		content.retain(increment);
		return this;
	}

	@Override
	public ReferenceCounted touch() {
		content.touch();
		return this;
	}

	@Override
	public ReferenceCounted touch(Object hint) {
		content.touch(hint);
		return this;
	}

	@Override
	public boolean release() {
		return content.release();
	}

	@Override
	public boolean release(int decrement) {
		return content.release(decrement);
	}

}
