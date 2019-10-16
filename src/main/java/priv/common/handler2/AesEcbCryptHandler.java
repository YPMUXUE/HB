package priv.common.handler2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import priv.common.crypto.AesCrypto;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * +---------------------encrypt--------------------------------------------+----------------------encrypt-------------------------+
 * |                                                                        |                                                      |
 * +------------------------------------------------------------------------+------------------------------------------------------+
 * |                 header(48)                                             |                    content                           |
 * |  0xXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                              |                   "0xXXXXXX"                         |
 * +------------------------------------------------------------------------+------------------------------------------------------+
 * <p>
 *              +---------------------Digest--------------------------------------------+------------Digest-------------+
 *              |                                                                       |                               |
 * +------------+----------------------+--------------------------+---------------------+-------------------------------+
 * | Digest(32) |  content Length(4)   |    protocol sign(12)     |    Digest(32)       |          content              |
 * |  0xXXXX    |    0x0000000X        |    0xXXXXXXXX            |    0xXXXXXXXX       |           "1234"              |
 * +------------+----------------------+--------------------------+---------------------+-------------------------------+
 * <p>
 *  
 *  
 *  
 *  
 */
public class AesEcbCryptHandler extends ByteToMessageDecoder implements ChannelOutboundHandler {
	private static final int FRAME_HEADER_LENGTH = 48;
	private static final int DIGEST_LENGTH = 32;
	private static final int CONTENT_LENGTH_OFFSET = DIGEST_LENGTH;
	private static final byte[] PROTOCOL_SIGN;
	private static final int PROTOCOL_SIGN_OFFSET = CONTENT_LENGTH_OFFSET + 4;

	private final AesCrypto aesCrypto;
	private byte[] headerBytes;

	static {
		PROTOCOL_SIGN = "123456789012".getBytes(StandardCharsets.US_ASCII);
		assert PROTOCOL_SIGN.length == 12;
	}

	public AesEcbCryptHandler(AesCrypto aesCrypto) {
		this.aesCrypto = aesCrypto;
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		Object decoded = decode(ctx, in);
		if (decoded != null) {
			out.add(decoded);
		}
	}

	/**
	 * @throws CorruptedFrameException
	 * @see io.netty.handler.codec.LengthFieldBasedFrameDecoder#decode(ChannelHandlerContext, ByteBuf)
	 */
	private ByteBuf splitFrame(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		if (headerBytes == null) {
			if (in.readableBytes() < FRAME_HEADER_LENGTH) {
				return null;
			}
			byte[] encryptedHeaderBytes = new byte[FRAME_HEADER_LENGTH];
			in.readBytes(FRAME_HEADER_LENGTH);
			this.headerBytes = this.aesCrypto.decrypt(encryptedHeaderBytes);
		}
		int frameLength = bytesToInt(headerBytes, CONTENT_LENGTH_OFFSET);
		if (frameLength <= 0) {
			throw new CorruptedFrameException("frame Length field is less then 0:" + frameLength);
		}
		if (!arraysEquals(this.headerBytes,PROTOCOL_SIGN_OFFSET,PROTOCOL_SIGN.length,PROTOCOL_SIGN)){
			throw new CorruptedFrameException("wrong sign of frame. headers:" + Hex.encodeHexString(headerBytes));
		}
		if (in.readableBytes() < frameLength) {
			return null;
		}


		return null;

	}

	private Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		ByteBuf frame = this.splitFrame(ctx, in);
		if (frame == null) {
			return null;
		}
		int contentLength = frame.readableBytes();
		byte[] encryptedContent = new byte[contentLength];
		frame.readBytes(encryptedContent);
		frame.release();
		byte[] decryptedContent = aesCrypto.decrypt(encryptedContent);
		return Unpooled.buffer(decryptedContent.length).writeBytes(decryptedContent);
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		try {
			if (!(msg instanceof ByteBuf)) {
				throw new IllegalArgumentException("msg is not a instance of ByteBuf");
			}
			ByteBuf buf = (ByteBuf) msg;
			int frameLength = buf.readableBytes();
			byte[] unencryptedContent = new byte[frameLength];
			buf.readBytes(unencryptedContent);
			byte[] encryptedContent = aesCrypto.encrypt(unencryptedContent);
			ByteBuf encryptedContentBuffer = ctx.alloc().buffer(FRAME_HEADER_LENGTH + encryptedContent.length);
			encryptedContentBuffer.writeInt(encryptedContent.length);
			encryptedContentBuffer.writeBytes(encryptedContent);
			ctx.write(encryptedContentBuffer, promise);
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void flush(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
		ctx.bind(localAddress, promise);
	}

	@Override
	public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
		ctx.connect(remoteAddress, localAddress, promise);
	}

	@Override
	public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
		ctx.disconnect(promise);
	}

	@Override
	public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
		ctx.close(promise);
	}

	@Override
	public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
		ctx.deregister(promise);
	}

	@Override
	public void read(ChannelHandlerContext ctx) throws Exception {
		ctx.read();
	}

	private static int bytesToInt(byte[] src, int offset) {
		int value;
		value = (((src[offset] & 0xFF) << 24)
				| ((src[offset + 1] & 0xFF) << 16)
				| ((src[offset + 2] & 0xFF) << 8)
				| (src[offset + 3] & 0xFF));
		return value;
	}
	private static boolean arraysEquals(byte[] array1,int array1Offset, int array1Length, byte[] array2) {
		if (array1 == null || array2 == null){
			return false;
		}
		if (array1Length != array2.length){
			return false;
		}
		for (int i = 0; i < array1Length; i++) {
			if (array1[i+array1Offset] != array2[i]){
				return false;
			}
		}
		return true;
	}
}
