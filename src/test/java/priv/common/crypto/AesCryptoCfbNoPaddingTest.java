package priv.common.crypto;


import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import priv.common.resource.StaticConfig;
import priv.common.resource.SystemConfig;

import java.util.Arrays;

public class AesCryptoCfbNoPaddingTest {
	public static void main(String[] args) throws Exception {
		byte[] key = Base64.decodeBase64(StaticConfig.AES_KEY);
		byte[] iv1 = DigestUtils.md5(key);
		AesCryptoCfbNoPadding aesCrypto = new AesCryptoCfbNoPadding(key,iv1);
		byte[] content = new byte[10*100];
		Arrays.fill(content, (byte) 123);
		long start = System.currentTimeMillis();

		byte[] encrypt1 = aesCrypto.encrypt(content);
		System.out.println("encrypt1:"+Hex.encodeHexString(encrypt1));
		content[0]= (byte) (content[0]+1);
		byte[] encrypt2= aesCrypto.encrypt(content);
		System.out.println("encrypt1:"+Hex.encodeHexString(encrypt2));
//		for (int i = 0; i < 100; i++) {
//
//		byte[] encrypt = aesCrypto.encrypt(content);
////		System.out.println("encrypt:"+Hex.encodeHexString(encrypt));
//		byte[] decrypt = aesCrypto.decrypt(encrypt);
////		System.out.println("decrypt:"+Hex.encodeHexString(decrypt));
//
//		}
		System.out.println(System.currentTimeMillis()-start);

	}
}