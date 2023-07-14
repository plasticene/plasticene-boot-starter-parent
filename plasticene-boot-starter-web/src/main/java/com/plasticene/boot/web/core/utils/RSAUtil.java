package com.plasticene.boot.web.core.utils;

import com.alibaba.fastjson.JSONObject;
import com.plasticene.boot.web.core.model.RequestInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2023/4/28 15:10
 */
@Slf4j
public class RSAUtil {
    public static final String KEY_ALGORITHM = "RSA";
    public static final String SIGNATURE_ALGORITHM = "SHA1WithRSA";
    public static final String ENCODING = "utf-8";
    public static final String X509 = "X.509";

    public static KeyPair getKeyPair(int keyLength) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");   //默认:RSA/None/PKCS1Padding
            keyPairGenerator.initialize(keyLength);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("生成密钥对时遇到异常" +  e.getMessage());
        }
    }

    public static byte[] getPublicKey(KeyPair keyPair) {
        RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
        return rsaPublicKey.getEncoded();
    }

    public static byte[] getPrivateKey(KeyPair keyPair) {
        RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
        return rsaPrivateKey.getEncoded();
    }

    /**
     * 获取公钥
     *
     * @param key
     * @return
     * @throws Exception
     */
    public static PublicKey getPublicKey(String key) throws Exception {
        byte[] keyBytes = Base64.decodeBase64(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
    }

    /**
     * 获取私钥
     *
     * @param key
     * @return
     * @throws Exception
     */
    public static PrivateKey getPrivateKey(String key) throws Exception {
        byte[] keyBytes = Base64.decodeBase64(key.getBytes(ENCODING));
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        return privateKey;
    }

    /**
     * RSA私钥签名
     *
     * @param content    待签名数据
     * @param privateKey 私钥
     * @return 签名值
     */
    public static String signByPrivateKey(String content, String privateKey) {
        try {
            PrivateKey priKey = getPrivateKey(privateKey);
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(priKey);
            signature.update(content.getBytes(ENCODING));
            byte[] signed = signature.sign();
            return new String(Base64.encodeBase64URLSafe(signed), ENCODING);
        } catch (Exception e) {
            log.warn("sign error, content: {}, priKey: {}", new Object[]{content, privateKey});
            log.error("sign error", e);
        }
        return null;
    }

    /**
     * RSA公钥验签
     *
     * @param content   待验签数据
     * @param publicKey 公钥
     * @return 是否成功
     */
    public static boolean verifySignByPublicKey(String content, String sign, String publicKey) {
        try {
            PublicKey pubKey = getPublicKey(publicKey);
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(pubKey);
            signature.update(content.getBytes(ENCODING));
            return signature.verify(Base64.decodeBase64(sign.getBytes(ENCODING)));
        } catch (Exception e) {

            log.warn("sign error, content: {}, sign: {}, pubKey: {}", new Object[]{content, sign, publicKey});
            log.error("sign error", e);
        }
        return false;
    }

    /**
     * 通过公钥对aes进⾏加密
     *
     * @param plainText 随机⽣成的aes key
     * @param publicKey 公钥
     * @return RSA加密后的aes key
     */
    public static String encryptByPublicKey(String plainText, String publicKey) {
        try {
            PublicKey pubKey = getPublicKey(publicKey);
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] enBytes = cipher.doFinal(plainText.getBytes(ENCODING));
            return Base64.encodeBase64String(enBytes);
        } catch (Exception e) {
            log.error("encrypt error", e);
        }
        return null;
    }

    /**
     * 通过私钥对aes进⾏加密
     *
     * @param enStr      加密后的aes key
     * @param privateKey 西撕私钥
     * @return RSA加密后的aes key
     */
    public static String decryptByPrivateKey(String enStr, String privateKey) {
        try {
            PrivateKey priKey = getPrivateKey(privateKey);
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, priKey);
            byte[] deBytes = cipher.doFinal(Base64.decodeBase64(enStr));
            return new String(deBytes);
        } catch (Exception e) {
            log.error("decrypt error", e);
        }
        return null;
    }

    public static void main(String[] args) {
//        KeyPair keyPair = getKeyPair(4096);
//        byte[] privateKey = getPrivateKey(keyPair);
//        byte[] publicKey = getPublicKey(keyPair);
//        String publicKeyString = Base64.encodeBase64String(publicKey);
//        System.out.println("public:" + publicKeyString);
//        // 得到私钥字符串
//        String privateKeyString = Base64.encodeBase64String(privateKey);
//        System.out.println("private:" + privateKeyString);
        String publicKeyString = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAxTqXxkqUfzrWO91G6Cja3CBoZ9Do3Sm/wKHmfe0UfIWebgWlW2juj4pJ98GPSoDf498CI7kOXiu0c8bDtAS34j5iuTsJKGbOE9FIbQn1yQW4myfXmsu/KKRn5vH30XDqN/kLowxfvS1WokE9qXGFySV0ZrJWxDUFNTtAEniKRn70W0oZ7aM2/weT0P8diU3Zm/+yi6pymTfTLLJm58wx3VBGnBCwUMppwNhkjZmcCfWEA89aNS/bwykjWGxDvczlT3mo8CaxUHjAy3Ei8zGqDMD4NzHkX6LL2yVB4orLvB8XDE3gjYr97q5R603/rODDtO1U/O4vFhX9XbZPjx3J2AWmI7yV4wlQ3zlrLY2jepGI8qFOrXoH9dLf0TbvsXFpG0ZHx5YGzkMH1/rFwnju7QlkLxMyxvXk0DDEn+bFrvjJc6B2GVOJ3shZdVBMC+Lq8H/RfYK89BfSnWTl1l8PWReNK/ESyR7LoCiDcnjcA8v/NZDTEpw1S7zNKBRo7KRfzWgRT//qN+nbDHjLX7VrpzvInOaried4DQ3umciysPhK0WhlQvqIbBYMXt9EnVlRvEfbdxyIdW3OWAF50izgwjRIaZ2nfL2M2Goc46F28an0+SrIqdwzMXuUPjvHvx2tuS3cVzEoaj2mubwR3p0UZksWZTnoHmqzsbel138ttpMCAwEAAQ==";
        String privateKeyString = "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";
        RequestInfo requestInfo = new RequestInfo();
//        requestInfo.setIp("127.0.0.1");
//        requestInfo.setUrl("/test/1");
        requestInfo.setHttpMethod("12");
        String s = JSONObject.toJSONString(requestInfo);
        String sign = signByPrivateKey(s, privateKeyString);
        boolean b = verifySignByPublicKey(s, sign, publicKeyString);
        System.out.println(b);


    }
}
