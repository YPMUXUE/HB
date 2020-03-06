package priv.common.message;

import io.netty.channel.Channel;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;

/**
 *  * @author  pyuan
 *  * @date    2019/9/17 0017
 *  * @Description
 *  *
 *  
 */
public class ChannelDataEntry implements ReferenceCounted {
	private final Channel bindChannel;
	private final Object data;

	public Channel getBindChannel() {
		return bindChannel;
	}

	public Object getData() {
		return data;
	}

	public ChannelDataEntry(Channel bindChannel, Object data) {
		this.bindChannel = bindChannel;
		this.data = data;
	}

	@Override
	public int refCnt() {
		return ReferenceCountUtil.refCnt(data);
	}

	@Override
	public ReferenceCounted retain() {
		ReferenceCountUtil.retain(data);
		return this;
	}

	@Override
	public ReferenceCounted retain(int increment) {
		ReferenceCountUtil.retain(data,increment);
		return this;
	}

	@Override
	public ReferenceCounted touch() {
		ReferenceCountUtil.touch(data);
		return this;
	}

	@Override
	public ReferenceCounted touch(Object hint) {
		ReferenceCountUtil.touch(data,hint);
		return this;
	}

	@Override
	public boolean release() {
		return ReferenceCountUtil.release(data);
	}

	@Override
	public boolean release(int decrement) {
		return ReferenceCountUtil.release(data,decrement);
	}
}
