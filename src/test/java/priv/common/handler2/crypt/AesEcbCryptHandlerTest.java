package priv.common.handler2.crypt;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import priv.common.crypto.AesCrypto;
import priv.common.crypto.AesCryptoEcbPKCS5Padding;
import priv.common.resource.StaticConfig;
import priv.common.resource.SystemConfig;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class AesEcbCryptHandlerTest {

	public static void main(String[] args) throws Exception {
		final byte[] keys = Base64.decodeBase64(StaticConfig.AES_KEY);
		AesCrypto aesCrypto = new AesCryptoEcbPKCS5Padding(keys);
		AesCryptoEcbPKCS5Padding aesCrypt = new AesCryptoEcbPKCS5Padding(keys);
		EmbeddedChannel testChannel ;
		final byte[] decryptContent = new byte[233];
		Arrays.fill(decryptContent,(byte)123);
		final byte[] encryptContent = aesCrypt.encrypt(decryptContent);

		//encode
		testChannel = new EmbeddedChannel(new AesEcbCryptHandler(aesCrypto));
		ByteBuf encodeByteBuf = Unpooled.buffer().writeBytes(decryptContent);
		testChannel.writeOutbound(encodeByteBuf);
		testChannel.finish();
		ByteBuf outboundBuf = testChannel.readOutbound();
		System.out.println(ByteBufUtil.equals(outboundBuf,Unpooled.buffer().writeInt(encryptContent.length).writeBytes(encryptContent)));

		//decode
		testChannel = new EmbeddedChannel(new AesEcbCryptHandler(aesCrypto));
		ByteBufUtil.hexDump(outboundBuf);
		testChannel.writeInbound(outboundBuf);
		testChannel.finish();
		ByteBuf result = testChannel.readInbound();
		System.out.println(ByteBufUtil.equals(result,Unpooled.buffer().writeBytes(decryptContent)));

	}

}
