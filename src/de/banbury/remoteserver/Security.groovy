package de.banbury.remoteserver

import java.util.prefs.Base64

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.PBEParameterSpec

class Security {
	private static final String algorithm = "PBEWithMD5AndDES"
	private static final byte[] salt = [
		67,
		222,
		18,
		174,
		59,
		243,
		69,
		125
	]

	private SecretKey key
	private Cipher cipher

	Security(String password) {
		SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
		char[] pass = password.toCharArray()
		key = factory.generateSecret(new PBEKeySpec(pass));
		cipher = Cipher.getInstance(algorithm);
		cipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(salt, 2048))
	}

	String decrypt(String msg) {
		byte[] buf = Base64.base64ToByteArray(msg)
		byte[] recoveredBytes =	cipher.doFinal(buf);
		return new String(recoveredBytes, "UTF-8");
	}
}
