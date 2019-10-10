package priv.common.handler2.crypt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import priv.common.resource.SystemConfig;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.SocketAddress;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.util.List;

public class AesEcbCryptHandler extends LengthFieldBasedFrameDecoder implements ChannelOutboundHandler {
	public static final String CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";
	public static final String KEY_ALGORITHM = "AES";
	private final SecretKey secretKey;

	public AesEcbCryptHandler(byte[] secretKey) {
		super(Integer.MAX_VALUE, 0,4,0,4);
		this.secretKey = new SecretKeySpec(secretKey, KEY_ALGORITHM);
	}

	public AesEcbCryptHandler(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip, byte[] secretKey) {
		super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
		this.secretKey = new SecretKeySpec(secretKey, KEY_ALGORITHM);
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		ByteBuf frame = (ByteBuf) super.decode(ctx, in);
		if (frame == null) {
			return null;
		}
		int contentLength = frame.readableBytes();
		byte[] encryptedContent = new byte[contentLength];
		frame.readBytes(encryptedContent);
		frame.release();
		byte[] decryptedContent = decrypt(encryptedContent, CIPHER_ALGORITHM, this.secretKey);
		return Unpooled.buffer(decryptedContent.length).writeBytes(decryptedContent);
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

	}

	@Override
	public void flush(ChannelHandlerContext ctx) throws Exception {

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

	private static byte[] encrypt(byte[] byteContent, String cipherAlgorithm, SecretKey secretKey) throws GeneralSecurityException {

		Cipher cipher = Cipher.getInstance(cipherAlgorithm);

		cipher.init(Cipher.ENCRYPT_MODE, secretKey);

		return cipher.doFinal(byteContent);
	}

	private static byte[] decrypt(byte[] content, String cipherAlgorithm, SecretKey secretKey) throws GeneralSecurityException {

		Cipher cipher = Cipher.getInstance(cipherAlgorithm);

		cipher.init(Cipher.DECRYPT_MODE, secretKey);

		return cipher.doFinal(content);
	}

}
