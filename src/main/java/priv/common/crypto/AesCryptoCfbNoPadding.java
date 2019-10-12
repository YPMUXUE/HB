package priv.common.crypto;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import priv.common.resource.StaticConfig;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;

/**
 *  * @author  pyuan
 *  * @date    2019/10/9 0009
 *  * @Description
 *  *
 *  
 */
public class AesCryptoCfbNoPadding implements AesCrypto {
	private static final String KEY_ALGORITHM = "AES";
	private String cipherAlgorithm = "AES/CFB/NoPadding";
	private final SecretKey secretKey;
	private final IvParameterSpec iv;

	public AesCryptoCfbNoPadding(byte[] key, byte[] iv) {
		Objects.requireNonNull(key);
		this.secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
		this.iv = new IvParameterSpec(iv);
	}
	public AesCryptoCfbNoPadding(byte[] key) {
		Objects.requireNonNull(key);
		this.secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
		this.iv = new IvParameterSpec(DigestUtils.md5(key));
	}

	@Override
	public byte[] encrypt(byte[] byteContent) throws GeneralSecurityException {

		Cipher cipher = Cipher.getInstance(cipherAlgorithm);

		cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

		return cipher.doFinal(byteContent);
	}

	@Override
	public byte[] decrypt(byte[] content) throws GeneralSecurityException {

		Cipher cipher = Cipher.getInstance(cipherAlgorithm);

		cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

		return cipher.doFinal(content);
	}
}
