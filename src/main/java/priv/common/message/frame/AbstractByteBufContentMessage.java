package priv.common.message.frame;


import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;

public abstract class AbstractByteBufContentMessage implements ContentMessage<ByteBuf> , ReferenceCounted {
	protected ByteBuf content;

	@Override
	public ByteBuf getContent() {
		return content;
	}

	@Override
	public void setContent(ByteBuf content) {
		this.content = content;
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
