package de.catma.api.crypto;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * @author Joe Prasanna Kumar (original implementation)
 * @auhor Marco Petris (minor changes)
 * 
 * This program provides the following cryptographic functionalities
 * 1. Encryption using AES
 * 2. Decryption using AES
 * 
 * High Level Algorithm :
 * 1. Generate a AES key (specify the Key size during this phase) 
 * 2. Create the Cipher 
 * 3. To Encrypt : Initialize the Cipher for Encryption
 * 4. To Decrypt : Initialize the Cipher for Decryption
 * <br><br>
 * See <a href="https://www.owasp.org/index.php/Using_the_Java_Cryptographic_Extensions">https://www.owasp.org/index.php/Using_the_Java_Cryptographic_Extensions</a>
 * for original implementation.
 * <br>
 * License: http://creativecommons.org/licenses/by-sa/3.0/
 */
public class AES {
	
	private SecretKey secretKey;
	private byte[] iv;

	public AES() {
		try{
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(128);
			secretKey = keyGen.generateKey();
			final int AES_KEYLENGTH = 128;	// change this as desired for the security level you want
			iv = new byte[AES_KEYLENGTH / 8];	// Save the IV bytes or send it in plaintext with the encrypted data so you can decrypt the data later
			/**
			 * Step 2. Generate an Initialization Vector (IV) 
			 * 		a. Use SecureRandom to generate random bits
			 * 		   The size of the IV matches the blocksize of the cipher (128 bits for AES)
			 * 		b. Construct the appropriate IvParameterSpec object for the data to pass to Cipher's init() method
			 */

			SecureRandom prng = SecureRandom.getInstance("SHA1PRNG", "SUN");
			prng.nextBytes(iv);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	public String encrypt(String strDataToEncrypt) {
		try {
			/**
			 * Step 3. Create a Cipher by specifying the following parameters
			 * 		a. Algorithm name - here it is AES 
			 * 		b. Mode - here it is CBC mode 
			 * 		c. Padding - e.g. PKCS7 or PKCS5
			 */

			Cipher aesCipherForEncryption = Cipher.getInstance("AES/CBC/PKCS5PADDING"); // Must specify the mode explicitly as most JCE providers default to ECB mode!!

			/**
			 * Step 4. Initialize the Cipher for Encryption
			 */

			aesCipherForEncryption.init(Cipher.ENCRYPT_MODE, secretKey, 
					new IvParameterSpec(iv));

			/**
			 * Step 5. Encrypt the Data 
			 * 		a. Declare / Initialize the Data. Here the data is of type String 
			 * 		b. Convert the Input Text to Bytes 
			 * 		c. Encrypt the bytes using doFinal method
			 */

			byte[] byteDataToEncrypt = strDataToEncrypt.getBytes();
			byte[] byteCipherText = aesCipherForEncryption
					.doFinal(byteDataToEncrypt);
			
			return new BASE64Encoder().encode(byteCipherText);

		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	public String decrypt(String strCipherText) {
		try {
			/**
			 * Step 6. Decrypt the Data 
			 * 		a. Initialize a new instance of Cipher for Decryption (normally don't reuse the same object)
			 * 		   Be sure to obtain the same IV bytes for CBC mode.
			 * 		b. Decrypt the cipher bytes using doFinal method
			 */
	
			Cipher aesCipherForDecryption = Cipher.getInstance("AES/CBC/PKCS7PADDING"); // Must specify the mode explicitly as most JCE providers default to ECB mode!!				
	
			aesCipherForDecryption.init(Cipher.DECRYPT_MODE, secretKey,
					new IvParameterSpec(iv));
			byte[] byteCipherText = new BASE64Decoder().decodeBuffer(strCipherText);
			byte[] byteDecryptedText = aesCipherForDecryption
					.doFinal(byteCipherText);
			return  new String(byteDecryptedText);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}