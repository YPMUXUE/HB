package priv.common.crypto;

import org.apache.commons.codec.binary.Base64;
import priv.common.resource.StaticConfig;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 *  * @author  pyuan
 *  * @date    2019/10/9 0009
 *  * @Description
 *  *
 *  
 */
public class AesCryptoEcbPKCS5Padding implements AesCrypto {
	private static final String KEY_ALGORITHM = "AES";
//	NoPadding
	private String cipherAlgorithm = "AES/ECB/PKCS5Padding";
	private final SecretKey secretKey;

	public AesCryptoEcbPKCS5Padding(byte[] key) {
		this.secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
	}

	@Override
	public byte[] encrypt(byte[] byteContent) throws GeneralSecurityException {

		Cipher cipher = Cipher.getInstance(cipherAlgorithm);

		cipher.init(Cipher.ENCRYPT_MODE, secretKey);

		return cipher.doFinal(byteContent);
	}

	@Override
	public byte[] decrypt(byte[] content) throws GeneralSecurityException {

		Cipher cipher = Cipher.getInstance(cipherAlgorithm);

		cipher.init(Cipher.DECRYPT_MODE, secretKey);

		return cipher.doFinal(content);
	}

	public static void main(String[] args) throws Exception {
		byte[] content = new byte[15 * 20];
		Arrays.fill(content,(byte)123);
		SecretKey secretKey = new SecretKeySpec(Base64.decodeBase64(StaticConfig.AES_KEY), "AES");

		long start = System.currentTimeMillis();
		for (int i = 0; i < 1; i++) {
			//encode
			Cipher cipher = Cipher.getInstance("AES/CFB/NoPadding");

			byte[] IV = new byte[16];
			Arrays.fill(IV, (byte) 123);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(IV));

			byte[] encrypt = cipher.doFinal(content);
			System.out.println("encrypt"+encrypt.length);
			//decode
			cipher = Cipher.getInstance("AES/CFB/NoPadding");

			cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(IV));

			byte[] decrypt = cipher.doFinal(encrypt);
			System.out.println("decrypt"+decrypt.length);
		}
		System.out.println(System.currentTimeMillis()-start);
	}
}
