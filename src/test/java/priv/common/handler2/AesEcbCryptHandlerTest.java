package priv.common.handler2;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import priv.common.crypto.AesCryptoCfbNoPadding;
import priv.common.resource.StaticConfig;
import priv.common.resource.SystemConfig;

import javax.xml.transform.Result;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public class AesEcbCryptHandlerTest {

	private static final AesCryptoCfbNoPadding aes = new AesCryptoCfbNoPadding(Base64.decodeBase64(StaticConfig.AES_KEY));

	public static void main(String[] args)throws Exception {
		long start = System.currentTimeMillis();
		EmbeddedChannel embeddedChannel;
		for (int i = 0; i < 1; i++) {
			byte[] content = new byte[123];
			Arrays.fill(content, (byte) 123);
			ByteBuf in = Unpooled.buffer(48 + 32 + content.length);
			encrypt(in,content);

			embeddedChannel = new EmbeddedChannel(new AesEcbCryptHandler(aes));
			embeddedChannel.writeInbound(in);
			embeddedChannel.finish();
			ByteBuf o = embeddedChannel.readInbound();
			int x = o.readableBytes();
			byte[] result = new byte[x];
			o.readBytes(result);
			System.out.println(Arrays.equals(result, content));
//			System.gc();

		}
		long end = System.currentTimeMillis();
		System.out.println(start - end);


		for (int i = 0; i < 1; i++) {
			byte[] content = new byte[123];
			Arrays.fill(content, (byte) 123);
			ByteBuf in = Unpooled.buffer(48 + 32 + content.length);
			encrypt(in,content);
			embeddedChannel = new EmbeddedChannel(new AesEcbCryptHandler(aes));

			embeddedChannel.writeOutbound(Unpooled.buffer().writeBytes(content));
			embeddedChannel.finish();
			ByteBuf outbound = embeddedChannel.readOutbound();
			System.out.println(ByteBufUtil.equals(outbound,in));
		}
	}

	private static void encrypt(ByteBuf in,byte[] content) throws Exception{

		MessageDigest sha256Digest = DigestUtils.getSha256Digest();
		byte[] contentDigest = sha256Digest.digest(content);
		sha256Digest.reset();
		sha256Digest.update(AesEcbCryptHandler.toBytes(content.length + 32));
		sha256Digest.update("123456789012".getBytes(StandardCharsets.US_ASCII));
		sha256Digest.update(contentDigest);
		byte[] headerDigest = sha256Digest.digest();
		sha256Digest.reset();

		byte[] decryptHeader = new byte[48];
		System.arraycopy(headerDigest, 0, decryptHeader, 0, headerDigest.length);
		System.arraycopy(AesEcbCryptHandler.toBytes(32 + content.length), 0, decryptHeader, 32, 4);
		System.arraycopy("123456789012".getBytes(StandardCharsets.US_ASCII), 0, decryptHeader, 32+4, 12);
		byte[] encryptHeader = aes.encrypt(decryptHeader);
		assert encryptHeader.length == 48;
		in.writeBytes(encryptHeader);
		byte[] decryptContent = new byte[32+content.length];
		System.arraycopy(contentDigest,0,decryptContent,0,contentDigest.length);
		System.arraycopy(content,0,decryptContent,headerDigest.length,content.length);
		byte[] encryptContent = aes.encrypt(decryptContent);
		in.writeBytes(encryptContent);
	}
}