package priv.common.crypto;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/**
 *  * @author  pyuan
 *  * @date    2019/10/11 0011
 *  * @Description
 *  *
 *  
 */
public interface AesCrypto {
	public static final String KEY_ALGORITHM = "AES";

	public byte[] encrypt(byte[] byteContent) throws Exception;

	public byte[] decrypt(byte[] content) throws Exception;

	public static byte[] generateKey(String pwd, int keyLength) throws GeneralSecurityException {
		KeyGenerator kg = KeyGenerator.getInstance(KEY_ALGORITHM);
		//128 192 256
		kg.init(keyLength, new SecureRandom(pwd.getBytes()));

		SecretKey secretKey = kg.generateKey();

		return secretKey.getEncoded();
	}
}
