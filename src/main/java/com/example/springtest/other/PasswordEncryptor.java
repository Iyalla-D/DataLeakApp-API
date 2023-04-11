package com.example.springtest.other;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;

import java.util.Map;
import java.util.Arrays;
import java.util.Base64;

@RestController
public class PasswordEncryptor {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA1";
	private static final int ITERATIONS = 10000;  // Number of iterations used in the PBKDF2 algorithm
    private static final int SALT_LENGTH = 16;    // Length of the salt used in the PBKDF2 algorithm
    private static final int KEY_LENGTH = 128;    // Length of the key generated by the PBKDF2 algorithm
    private static Cipher cipher;
  
    @PostConstruct
    public void init() throws Exception {
        cipher = Cipher.getInstance(ALGORITHM);
    }

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    private static byte[] generateKey(char[] password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRET_KEY_FACTORY_ALGORITHM);
        return factory.generateSecret(spec).getEncoded();
    }

    @PostMapping("/encrypt")
    public String encryptObj(@RequestBody Map<String, String> data) {
        String Obj = data.get("Obj");
        String masterPassword = data.get("master");
        try {
            byte[] salt = generateSalt();
            byte[] key = generateKey(masterPassword.toCharArray(), salt);

            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(Arrays.copyOfRange(salt, 0, 16));
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encryptedPassword = cipher.doFinal(Obj.getBytes("UTF-8"));
            byte[] saltAndEncryptedPassword = new byte[SALT_LENGTH + encryptedPassword.length];
            System.arraycopy(salt, 0, saltAndEncryptedPassword, 0, SALT_LENGTH);
            System.arraycopy(encryptedPassword, 0, saltAndEncryptedPassword, SALT_LENGTH,
            encryptedPassword.length);

            return Base64.getEncoder().encodeToString(saltAndEncryptedPassword);

        }catch(Exception e){
            e.printStackTrace();
            return null;
        }

    }

    @PostMapping("/decrypt")
    public static String decryptObj(@RequestBody Map<String, String> request) {
        String encodedObj = request.get("Obj");
        String masterPassword = request.get("master");
        
        try {
            byte[] saltAndEncryptedPassword = Base64.getDecoder().decode(encodedObj);
            byte[] salt = Arrays.copyOfRange(saltAndEncryptedPassword, 0, SALT_LENGTH);
            byte[] encryptedPassword = Arrays.copyOfRange(saltAndEncryptedPassword, SALT_LENGTH, saltAndEncryptedPassword.length);

            byte[] key = generateKey(masterPassword.toCharArray(), salt);
            
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(Arrays.copyOfRange(salt, 0, 16));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decryptedPassword = cipher.doFinal(encryptedPassword);

            return new String(decryptedPassword, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}