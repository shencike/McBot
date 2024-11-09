package org.example.Util.verify;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class StringUtil {

    public static byte[] ENCRYPT(byte[] string, String rsaKey, String aesKey){
        return RSAENCRYPT(shitEncrypt(string, aesKey), rsaKey);
    }

    public static byte[] DECRYPT(byte[] string, String rsaKey, String aesKey){
        return shitDecrypt(RSADECRYPT(string,rsaKey), aesKey);
    }

    public static byte[] RSAENCRYPT(byte[] string, String rsaKey){
        if (string == null) return null;
        try {
            //String k = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtDH0yMQUoHFQWMVCjFiZeyzOa3K8Gy8U5mntpoC2rL/quMLUxXJOCYSmjALBWHduqjoxVie1IGScar52J0KvunoRw+zhabxNJpdmSrnNsbkpEHi/zDjpy8bGH4i0xAls2jvWba2nDHRdYn7XrS5Od8J3YFvBC+sICrsMQL7RrXPVfzHMqnu1oB0LFdY438NOqpBlyE/WeuXF0O2Uf7IzGD6wOS6ervGaYpGoRf4nIcnJwX6bhPtj2sDl+aUmS9EMWaA8kmJTsGCecj2/EQN+s3JzWO+QiDOLkzT9cETeMzqWAW4ljZf9wTKhesOGnIPTDMdXbGtbJxqPbkZlvmCc6wIDAQAB";
            X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.getDecoder().decode(rsaKey));
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey publicKey = kf.generatePublic(spec);

            Cipher encryptCipher = Cipher.getInstance("RSA");
            encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            int inputLength = string.length;
            List<byte[]> encryptedData = new ArrayList<>();
            int offset = 0;

            while (offset < inputLength) {
                int length = Math.min(inputLength - offset, 2048 / 8 - 11);
                byte[] encryptedBlock = encryptCipher.doFinal(string, offset, length);
                encryptedData.add(encryptedBlock);
                offset += length;
            }
            int totalLength = encryptedData.stream().mapToInt(b -> b.length).sum();
            byte[] result = new byte[totalLength];
            int currentPosition = 0;
            for (byte[] block : encryptedData) {
                System.arraycopy(block, 0, result, currentPosition, block.length);
                currentPosition += block.length;
            }
            return result;
        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | NoSuchAlgorithmException |
                 InvalidKeySpecException | InvalidKeyException ignored) {

        }
        return null;
    }

    public static byte[] RSADECRYPT(byte[] string, String rsaKey){
        if (string == null) return null;
        try {
            //String k = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtDH0yMQUoHFQWMVCjFiZeyzOa3K8Gy8U5mntpoC2rL/quMLUxXJOCYSmjALBWHduqjoxVie1IGScar52J0KvunoRw+zhabxNJpdmSrnNsbkpEHi/zDjpy8bGH4i0xAls2jvWba2nDHRdYn7XrS5Od8J3YFvBC+sICrsMQL7RrXPVfzHMqnu1oB0LFdY438NOqpBlyE/WeuXF0O2Uf7IzGD6wOS6ervGaYpGoRf4nIcnJwX6bhPtj2sDl+aUmS9EMWaA8kmJTsGCecj2/EQN+s3JzWO+QiDOLkzT9cETeMzqWAW4ljZf9wTKhesOGnIPTDMdXbGtbJxqPbkZlvmCc6wIDAQAB";
            X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.getDecoder().decode(rsaKey));
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey publicKey = kf.generatePublic(spec);

            Cipher decryptCipher = Cipher.getInstance("RSA");
            decryptCipher.init(Cipher.DECRYPT_MODE, publicKey);
            int inputLength = string.length;
            List<byte[]> encryptedData = new ArrayList<>();
            int offset = 0;

            while (offset < inputLength) {
                int length = Math.min(inputLength - offset,  2048 / 8);
                byte[] encryptedBlock = decryptCipher.doFinal(string, offset, length);
                encryptedData.add(encryptedBlock);
                offset += length;
            }
            int totalLength = encryptedData.stream().mapToInt(b -> b.length).sum();
            byte[] result = new byte[totalLength];
            int currentPosition = 0;
            for (byte[] block : encryptedData) {
                System.arraycopy(block, 0, result, currentPosition, block.length);
                currentPosition += block.length;
            }
            return result;
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                 InvalidKeySpecException | BadPaddingException | InvalidKeyException ignored) {
        }
        return null;
    }

    public static byte[] shitEncrypt(byte[] bytes, String aesKey) {
        if (bytes == null) return null;
        try {
            int aesKeyLength = 32;
            byte[] aesKeyBytes = new byte[aesKeyLength];
            System.arraycopy(aesKey.getBytes(), 0, aesKeyBytes, 0, aesKeyLength);
            SecretKeySpec spec = new SecretKeySpec(aesKeyBytes,"AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE,spec);
            return cipher.doFinal(bytes);
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException |
                 InvalidKeyException ignored) {
        }
        return null;
    }

    public static byte[] shitDecrypt(byte[] bytes, String aesKey) {
        if (bytes == null) return null;
        try {
            int aesKeyLength = 32;
            byte[] aesKeyBytes = new byte[aesKeyLength];
            System.arraycopy(aesKey.getBytes(), 0, aesKeyBytes, 0, aesKeyLength);
            SecretKeySpec spec = new SecretKeySpec(aesKeyBytes,"AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE,spec);
            return cipher.doFinal(bytes);
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException |
                 InvalidKeyException ignored) {
        }
        return null;
    }
}
