package priv.common.connection;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ReferenceCountUtil;

/**
 *  * @author  pyuan
 *  * @date    2020/4/27 0027
 *  * @Description
 *  *
 *  
 */
public class ByteBufCache {
	public static final ByteToMessageDecoder.Cumulator MERGE_CUMULATOR = ByteToMessageDecoder.MERGE_CUMULATOR;
	public static final ByteToMessageDecoder.Cumulator COMPOSITE_CUMULATOR = ByteToMessageDecoder.COMPOSITE_CUMULATOR;


	private static final ByteBufAllocator DEFAUL_ALLOC = new PooledByteBufAllocator(false);
	private ByteBuf outputBuffer;
	private final ByteToMessageDecoder.Cumulator cumulator;
	private final ByteBufAllocator allocator;
	private boolean close;

	public ByteBufCache(){
		this.cumulator = MERGE_CUMULATOR;
		this.allocator = DEFAUL_ALLOC;
	}

	public ByteBuf get() {
		ByteBuf result;
			result = this.outputBuffer;
			this.outputBuffer = null;
			if (result == null){
				result = new EmptyByteBuf(allocator);
			}
		return result;
	}

	public void writeBuffer(ByteBuf data) {
		if (isClose()){
			ReferenceCountUtil.release(data);
			return;
		}
			ByteBuf cumulation  = this.outputBuffer;
			if (cumulation == null){
				cumulation = data;
			}else{
				cumulation = cumulator.cumulate(allocator, cumulation, data);
			}
			this.outputBuffer = cumulation;

	}
	public int size(){
		int i=0;
			ByteBuf outputBuffer = this.outputBuffer;
			i = outputBuffer == null ? 0 : outputBuffer.readableBytes();
		return i;
	}
	public void clear(){
		ByteBuf byteBuf;
			byteBuf = this.outputBuffer;
			this.outputBuffer = null;
		ReferenceCountUtil.release(byteBuf);
	}
	public boolean isClose(){
		return close;
	}
	public void close(boolean clearBuffer){
		this.close = true;
		if (clearBuffer){
			this.clear();
		}
	}
}
