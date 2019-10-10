package priv.common.handler2.crypt;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import priv.common.crypto.AesCryptoEcbPKCS5Padding;
import priv.common.resource.SystemConfig;

import java.util.Arrays;

public class AesEcbCryptHandlerTest {

	public static void main(String[] args) throws Exception {
		final byte[] keys = AesCryptoEcbPKCS5Padding.generateKey("yuanpan", 128);
		AesCryptoEcbPKCS5Padding aesCrypt = new AesCryptoEcbPKCS5Padding(keys);
		EmbeddedChannel testChannel = new EmbeddedChannel(new AesEcbCryptHandler(SystemConfig.PACKAGE_MAX_LENGTH,0,4,0,4,keys));
		final byte[] decryptContent = new byte[233];
		Arrays.fill(decryptContent,(byte)123);
		final byte[] encryptContent = aesCrypt.encrypt(decryptContent);
		ByteBuf byteBuf = Unpooled.buffer().writeInt(encryptContent.length).writeBytes(encryptContent);
		ByteBufUtil.hexDump(byteBuf);
		testChannel.writeInbound(byteBuf);
		testChannel.finish();
		ByteBuf result = testChannel.readInbound();
		System.out.println(ByteBufUtil.equals(result,Unpooled.buffer().writeBytes(decryptContent)));
	}

}
