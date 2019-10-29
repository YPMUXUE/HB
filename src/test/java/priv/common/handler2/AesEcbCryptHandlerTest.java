package priv.common.handler2;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import priv.common.crypto.AesCryptoCfbNoPadding;
import priv.common.resource.StaticConfig;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public class AesEcbCryptHandlerTest {

	private static final AesCryptoCfbNoPadding aes = new AesCryptoCfbNoPadding(Base64.decodeBase64(StaticConfig.AES_KEY));

	public static void main(String[] args)throws Exception {
		AesCryptoCfbNoPadding aesCryptoCfbNoPadding = new AesCryptoCfbNoPadding(Base64.decodeBase64(StaticConfig.AES_KEY));
		byte[] decrypt = aesCryptoCfbNoPadding.decrypt(Hex.decodeHex("089f7fbfe2eb2fb98a3ac7e21ba393590da1e69710f7ecc1f5779b04855c7fa0c4f2ad4209937ac3aa81f0705303a1a5"));
		System.out.println(Arrays.toString(decrypt));
	}

	public static void main1(String[] args)throws Exception {
		long start = System.currentTimeMillis();
		EmbeddedChannel embeddedChannel;
		for (int i = 0; i < 1; i++) {
			byte[] content = new byte[123];
			Arrays.fill(content, (byte) 123);
			byte[] encrypt = encrypt(content);

			embeddedChannel = new EmbeddedChannel(new AesEcbCryptHandler(Base64.decodeBase64(StaticConfig.AES_KEY)));

			embeddedChannel.writeInbound(Unpooled.buffer().writeBytes(encrypt));

			embeddedChannel.writeInbound(Unpooled.buffer().writeBytes(encrypt,0,10));
			embeddedChannel.writeInbound(Unpooled.buffer().writeBytes(encrypt,10,encrypt.length-10));

			embeddedChannel.writeInbound(Unpooled.buffer().writeBytes(encrypt,0,10));
			embeddedChannel.writeInbound(Unpooled.buffer().writeBytes(encrypt,10,10));
			embeddedChannel.writeInbound(Unpooled.buffer().writeBytes(encrypt,20,encrypt.length-20));

			embeddedChannel.writeInbound(Unpooled.buffer().writeBytes(encrypt,0,100));
			embeddedChannel.writeInbound(Unpooled.buffer().writeBytes(encrypt,100,encrypt.length-100));
			embeddedChannel.finish();
			ByteBuf o = embeddedChannel.readInbound();
			int x = o.readableBytes();
			byte[] result = new byte[x];
			o.readBytes(result);
			assert Arrays.equals(result, content);
			for (int j = 0; j < 2; j++) {
				ByteBuf o1 = embeddedChannel.readInbound();
				assert ByteBufUtil.equals(o,o1);
			}
		}
		long end = System.currentTimeMillis();
		System.out.println(start - end);


		for (int i = 0; i < 1; i++) {
			byte[] content = new byte[123];
			Arrays.fill(content, (byte) 123);
			ByteBuf in = Unpooled.buffer(48 + 32 + content.length);
			byte[] encrypt = encrypt(content);
			in.writeBytes(encrypt);
			embeddedChannel = new EmbeddedChannel(new AesEcbCryptHandler(Base64.decodeBase64(StaticConfig.AES_KEY)));

			embeddedChannel.writeOutbound(Unpooled.buffer().writeBytes(content));
			embeddedChannel.finish();
			ByteBuf outbound = embeddedChannel.readOutbound();
			System.out.println(ByteBufUtil.equals(outbound,in));
		}
	}

	private static byte[] encrypt(byte[] content) throws Exception{
		byte[] result = new byte[content.length +48+32];
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

		byte[] decryptContent = new byte[32+content.length];
		System.arraycopy(contentDigest,0,decryptContent,0,contentDigest.length);
		System.arraycopy(content,0,decryptContent,headerDigest.length,content.length);
		byte[] encryptContent = aes.encrypt(decryptContent);
//		in.writeBytes(encryptHeader);
		System.arraycopy(encryptHeader,0,result,0,encryptHeader.length);
//		in.writeBytes(encryptContent);
		System.arraycopy(encryptContent,0,result,encryptHeader.length,encryptContent.length);
		return result;
	}
}