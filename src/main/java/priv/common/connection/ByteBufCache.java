package priv.common.connection;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.ByteToMessageDecoder;

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

	public ByteBufCache(){
		this.cumulator = MERGE_CUMULATOR;
		this.allocator = DEFAUL_ALLOC;
	}

	public ByteBuf get() {
		ByteBuf result;
		synchronized (this){
			result = this.outputBuffer;
			this.outputBuffer = null;
		}
		return result;
	}

	public void writeBuffer(ByteBuf data) {
		synchronized (this){
			ByteBuf cumulation  = this.outputBuffer;
			if (cumulation == null){
				cumulation = data;
			}else{
				cumulation = cumulator.cumulate(allocator, cumulation, data);
			}
			this.outputBuffer = cumulation;
		}

	}
	public int size(){
		int i=0;
		synchronized (this){
			ByteBuf outputBuffer = this.outputBuffer;
			i = outputBuffer == null ? 0 : outputBuffer.readableBytes();
		}
		return i;
	}
	public ByteBuf clear(){
		ByteBuf byteBuf = null;
		synchronized (this){
			ByteBuf outputBuffer = this.outputBuffer;
			this.outputBuffer = null;
		}
		return byteBuf;
	}
}
