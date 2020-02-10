package priv.common.handler2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import priv.common.crypto.AesCrypto;
import priv.common.crypto.AesCryptoCfbNoPadding;
import priv.common.resource.StaticConfig;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
	private static final Logger logger = LoggerFactory.getLogger(AesEcbCryptHandler.class);
	private static final int FRAME_HEADER_LENGTH = 48;
	private static final int DIGEST_LENGTH = 32;
	private  static final int CONTENT_LENGTH_FIELD_LENGTH = 4;
	private static final int CONTENT_LENGTH_OFFSET = DIGEST_LENGTH;
	private static final byte[] PROTOCOL_SIGN;
	private static final int PROTOCOL_SIGN_OFFSET = CONTENT_LENGTH_OFFSET + CONTENT_LENGTH_FIELD_LENGTH;

	private final AesCrypto aesCrypto;
	private byte[] headerBytes;
	private boolean decodeHeader = true;

	static {
		PROTOCOL_SIGN = "123456789012".getBytes(StandardCharsets.US_ASCII);
		assert PROTOCOL_SIGN.length == 12;
	}

	public AesEcbCryptHandler(byte[] key) {
		this.aesCrypto = new AesCryptoCfbNoPadding(key);
	}

	public AesEcbCryptHandler(byte[] key, byte[] iv) {
		this.aesCrypto = new AesCryptoCfbNoPadding(key, iv);
	}


	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		Object decoded = null;
		try {
			decoded = decode(ctx, in);
		} catch (CorruptedFrameException e) {
			this.headerBytes = null;
			this.decodeHeader = true;
			throw e;
		}
		if (decoded != null) {
			out.add(decoded);
		}
	}

	/**
	 * @throws CorruptedFrameException
	 * @see io.netty.handler.codec.LengthFieldBasedFrameDecoder#decode(ChannelHandlerContext, ByteBuf)
	 */
	private ByteBuf decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		if (decodeHeader) {
			if (in.readableBytes() < FRAME_HEADER_LENGTH) {
				return null;
			}
			byte[] encryptedHeaderBytes;
			if (this.headerBytes == null) {
				encryptedHeaderBytes = new byte[FRAME_HEADER_LENGTH];
			} else {
				encryptedHeaderBytes = this.headerBytes;
			}
			in.readBytes(encryptedHeaderBytes,0,FRAME_HEADER_LENGTH);
			this.headerBytes = this.aesCrypto.decrypt(encryptedHeaderBytes);
			checkHeaders(headerBytes);
			decodeHeader = false;
		}
		int encryptFrameLength = bytesToInt(headerBytes, CONTENT_LENGTH_OFFSET);

		if (in.readableBytes() < encryptFrameLength) {
			return null;
		}
		byte[] contentBytes = new byte[encryptFrameLength];
		in.readBytes(contentBytes);
		contentBytes = this.aesCrypto.decrypt(contentBytes);
		//头部摘要
		MessageDigest sha256Digest = DigestUtils.getSha256Digest();
		sha256Digest.update(headerBytes, CONTENT_LENGTH_OFFSET, CONTENT_LENGTH_FIELD_LENGTH + PROTOCOL_SIGN.length);
		sha256Digest.update(contentBytes, 0, DIGEST_LENGTH);
		byte[] headerDigest = sha256Digest.digest();
		if (!arraysEquals(headerBytes, 0, DIGEST_LENGTH, headerDigest)) {
			throw new CorruptedFrameException("wrong header digest:" + Hex.encodeHexString(headerBytes));
		}
		//内容摘要
		sha256Digest.reset();
		sha256Digest.update(contentBytes, DIGEST_LENGTH, contentBytes.length - DIGEST_LENGTH);
		byte[] contentDigest = sha256Digest.digest();
		if (!arraysEquals(contentBytes, 0, DIGEST_LENGTH, contentDigest)) {
			throw new CorruptedFrameException("wrong content digest");
		}
		int decryptedFrameLength = contentBytes.length - DIGEST_LENGTH;
		this.decodeHeader = true;
		if (logger.isDebugEnabled()){
			logger.debug("decryptedFrameLength:{},decode result:{}",DIGEST_LENGTH, Hex.encodeHexString(contentBytes));
		}
		return Unpooled.buffer(decryptedFrameLength).writeBytes(contentBytes, DIGEST_LENGTH, decryptedFrameLength);
	}

	private void checkHeaders(byte[] headerBytes) {
		if (headerBytes.length != FRAME_HEADER_LENGTH) {
			throw new CorruptedFrameException("wrong header length :" + headerBytes.length);
		}
		int frameLength = bytesToInt(headerBytes, CONTENT_LENGTH_OFFSET);
		if (frameLength <= 0) {
			throw new CorruptedFrameException("frame Length field is less then 0:" + frameLength);
		}
		if (!arraysEquals(this.headerBytes, PROTOCOL_SIGN_OFFSET, PROTOCOL_SIGN.length, PROTOCOL_SIGN)) {
			throw new CorruptedFrameException("wrong sign of frame. headers:" + Hex.encodeHexString(headerBytes));
		}
	}


	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		ByteBuf encode = encode(ctx, (ByteBuf) msg, promise);
		if (encode == null){
			ctx.write(Unpooled.EMPTY_BUFFER, promise);
		}else {
			ctx.write(encode, promise);
		}
	}

	private ByteBuf encode(ChannelHandlerContext ctx, ByteBuf buf, ChannelPromise promise) throws Exception {
		try {
			int frameLength = buf.readableBytes();
			if (frameLength == 0) {
				return null;
			}
			byte[] content = new byte[frameLength + DIGEST_LENGTH];
			buf.readBytes(content, DIGEST_LENGTH, frameLength);
			MessageDigest sha256Digest = DigestUtils.getSha256Digest();
			sha256Digest.update(content, DIGEST_LENGTH, content.length - DIGEST_LENGTH);
			byte[] contentDigest = sha256Digest.digest();
			sha256Digest.reset();
			System.arraycopy(contentDigest,0,content,0,contentDigest.length);
			final byte[] encryptedContent = aesCrypto.encrypt(content);
			content = null;


			byte[] headerBytes = new byte[FRAME_HEADER_LENGTH];
			System.arraycopy(toBytes(encryptedContent.length),0,headerBytes,CONTENT_LENGTH_OFFSET,CONTENT_LENGTH_FIELD_LENGTH);
			System.arraycopy(PROTOCOL_SIGN,0,headerBytes,PROTOCOL_SIGN_OFFSET,PROTOCOL_SIGN.length);
			sha256Digest.update(headerBytes,DIGEST_LENGTH,headerBytes.length - DIGEST_LENGTH);
			sha256Digest.update(contentDigest);
			byte[] headerDigest = sha256Digest.digest();
			sha256Digest.reset();
			contentDigest = null;
			System.arraycopy(headerDigest,0,headerBytes,0,DIGEST_LENGTH);
			final byte[] encryptedHeader = aesCrypto.encrypt(headerBytes);
			headerBytes = null;

			ByteBuf encryptedContentBuffer = buf.alloc().buffer(encryptedHeader.length + encryptedContent.length);
			encryptedContentBuffer.writeBytes(encryptedHeader).writeBytes(encryptedContent);
			return encryptedContentBuffer;
		} finally {
			ReferenceCountUtil.release(buf);
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

	public static int bytesToInt(byte[] src, int offset) {
		int value;
		value = (((src[offset] & 0xFF) << 24)
				| ((src[offset + 1] & 0xFF) << 16)
				| ((src[offset + 2] & 0xFF) << 8)
				| (src[offset + 3] & 0xFF));
		return value;
	}
	public static byte[] toBytes(int number){
		byte[] bytes = new byte[4];
		bytes[0] =(byte) (number >> 24);
		bytes[1] = (byte) (number >> 16);
		bytes[2] = (byte) (number >> 8);
		bytes[3] = (byte)number;
		return bytes;
	}

	private static boolean arraysEquals(byte[] array1, int array1Offset, int array1Length, byte[] array2) {
		if (array1 == null || array2 == null) {
			return false;
		}
		if (array1Length != array2.length) {
			return false;
		}
		for (int i = 0; i < array1Length; i++) {
			if (array1[i + array1Offset] != array2[i]) {
				return false;
			}
		}
		return true;
	}

	public static void main(String[] args)throws Exception {
		byte[] key = Base64.decodeBase64(StaticConfig.AES_KEY);
		AesCryptoCfbNoPadding aes = new AesCryptoCfbNoPadding(key);
		byte[] decrypt = aes.decrypt(Hex.decodeHex("64c862e80ba9e44a13e9f4bbdd5af49b2367762b5b48b6c322618e570114e6a3b516cb66175a3be1bff5308c36448455"));
		System.out.println(Hex.encodeHexString(decrypt));
	}
}
