package priv.common.message;

import io.netty.channel.Channel;
import io.netty.util.ReferenceCounted;

/**
 *  * @author  pyuan
 *  * @date    2019/9/17 0017
 *  * @Description
 *  *
 *  
 */
public class ChannelDataEntry<T extends ReferenceCounted> implements ReferenceCounted {
	private final Channel bindChannel;
	private final T data;

	public Channel getBindChannel() {
		return bindChannel;
	}

	public T getData() {
		return data;
	}

	public ChannelDataEntry(Channel bindChannel, T data) {
		this.bindChannel = bindChannel;
		this.data = data;
	}

	@Override
	public int refCnt() {
		return data.refCnt();
	}

	@Override
	public ReferenceCounted retain() {
		return data.retain();
	}

	@Override
	public ReferenceCounted retain(int increment) {
		return data.retain(increment);
	}

	@Override
	public ReferenceCounted touch() {
		return data.touch();
	}

	@Override
	public ReferenceCounted touch(Object hint) {
		return data.touch(hint);
	}

	@Override
	public boolean release() {
		return data.release();
	}

	@Override
	public boolean release(int decrement) {
		return data.release(decrement);
	}
}
