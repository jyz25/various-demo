package org.example.util;


import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;

/**
 * @auther Kindow
 * @date 2024/7/11
 * @project springboot-weixin-qrcode-login
 */


public class AESUtils {

    public static SecretKey generateKey(int n) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(n); // for example, 128, 192 or 256 bit key size
        SecretKey secretKey = keyGen.generateKey();
        return secretKey;
    }

    // Method to convert SecretKey to String
    public static String encodeKey(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    // Method to convert String back to SecretKey
    public static SecretKey decodeKey(String encodedKey) {
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }

    // Method to encrypt a plain text
    public static String encrypt(String plainText, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(cipherText);
    }

    // Method to decrypt a cipher text
    public static String decrypt(String cipherText, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText));
        return new String(plainText, "UTF-8");
    }

    public static void main(String[] args) {

        try {

            SecretKey key = generateKey(128);
            String encodedKey = encodeKey(key);

            System.out.println("生成的秘钥是：" + encodedKey);


            String plainText = "Hello, World!";
            System.out.println("源文本是：" + plainText);


            String cipherText = encrypt(plainText, key);
            System.out.println("加密后的密文是：" + cipherText);


            String decryptedText = decrypt(cipherText, key);
            System.out.println("密文解密后是：" + decryptedText);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
