package priv.common.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
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
public class AesEcbCrypto {
	private static final String KEY_ALGORITHM = "AES";
//	NoPadding
	private String cipherAlgorithm = "AES/ECB/PKCS5Padding";
	private final SecretKey secretKey;

	public AesEcbCrypto(byte[] key) {
		this.secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
	}

	public byte[] encrypt(byte[] byteContent) throws GeneralSecurityException {

		Cipher cipher = Cipher.getInstance(cipherAlgorithm);

		cipher.init(Cipher.ENCRYPT_MODE, secretKey);

		return cipher.doFinal(byteContent);
	}

	public byte[] decrypt(byte[] content) throws GeneralSecurityException {

		Cipher cipher = Cipher.getInstance(cipherAlgorithm);

		cipher.init(Cipher.DECRYPT_MODE, secretKey);

		return cipher.doFinal(content);
	}

	public static void main(String[] args) throws Exception {
		byte[] content = new byte[16 * 100000];
		Arrays.fill(content,(byte)123);
		AesEcbCrypto aesCrypto = new AesEcbCrypto(generateKey("123",128));
		long start = System.currentTimeMillis();
		for (int i = 0; i < 1; i++) {
			byte[] encrypt = aesCrypto.encrypt(content);
//			System.out.println("encrypt"+encrypt.length);
			byte[] decrypt = aesCrypto.decrypt(encrypt);
//			System.out.println("decrypt"+decrypt.length);
		}
		System.out.println(System.currentTimeMillis()-start);
	}

	public static byte[] generateKey(String pwd, int keyLength) throws GeneralSecurityException {

		KeyGenerator kg = KeyGenerator.getInstance(KEY_ALGORITHM);

		//128 192 256
		kg.init(keyLength, new SecureRandom(pwd.getBytes()));

		SecretKey secretKey = kg.generateKey();

		return secretKey.getEncoded();
	}
}
