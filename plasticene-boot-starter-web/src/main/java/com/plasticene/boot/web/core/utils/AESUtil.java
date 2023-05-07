package com.plasticene.boot.web.core.utils;

import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2023/5/4 10:51
 */
public class AESUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(AESUtil.class);
    public static final String KEY_ALGORITHM = "AES";
    public static final String ENCODING = "utf-8";


    /**
     * 随机生成aes key
     * @return
     */
    public static String generateAESKey() {
        KeyGenerator keyGenerator = null;
        try {
            keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        keyGenerator.init(128);
        SecretKey key = keyGenerator.generateKey();
        byte[] keyExternal = key.getEncoded();
        return Base64.encodeBase64String(keyExternal);
    }

    /**
     * 加密
     * @param content
     * @param key
     * @return
     */
    public static String encrypt(String content, String key) {
        try {
            byte[] bytesKey = Base64.decodeBase64(key);
            SecretKeySpec secretKey = new SecretKeySpec(bytesKey, KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);// 创建密码器
            byte[] byteContent = content.getBytes(ENCODING);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);// 初始化
            byte[] result = cipher.doFinal(byteContent);// 加密
            return Base64.encodeBase64String(result);
        } catch (Exception e) {
            LOGGER.error("encrypt error", e);
        }
        return null;
    }

    /**
     * 解密
     * @param content
     * @param key
     * @return
     */
    public static String decrypt(String content, String key) {
        try {
            byte[] bytesKey = Base64.decodeBase64(key);
            SecretKeySpec secretKey = new SecretKeySpec(bytesKey, KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);// 创建密码器
            cipher.init(Cipher.DECRYPT_MODE, secretKey);// 初始化
            byte[] result = cipher.doFinal(Base64.decodeBase64(content));// 解密
            return new String(result);
        } catch (Exception e) {
            LOGGER.error("decrypt error", e);
        }
        return null;
    }

    public static void main(String[] args) {
        String s = AESUtil.generateAESKey();
        System.out.println(s);
    }
}
