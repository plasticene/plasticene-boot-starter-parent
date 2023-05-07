package com.plasticene.boot.web.core.utils;

import com.alibaba.fastjson.JSONObject;
import com.plasticene.boot.web.core.model.RequestInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
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
            log.warn("sign error, content: {}, privateKey: {}", new Object[]{content, privateKey});
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
            return signature.verify(Base64.decodeBase64URLSafe(new String(sign.getBytes(), ENCODING)));
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
        KeyPair keyPair = getKeyPair(4096);
        byte[] privateKey = getPrivateKey(keyPair);
        byte[] publicKey = getPublicKey(keyPair);
        String publicKeyString = Base64.encodeBase64String(publicKey);
        System.out.println("public:" + publicKeyString);
        // 得到私钥字符串
        String privateKeyString = Base64.encodeBase64String(privateKey);
        System.out.println("private:" + privateKeyString);
//        String publicKeyString= "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAu/SJl2VWDF8RLyvIspjFQMiaoKrgJ9dQ+gm1Cqf/itg25BpP0Y5otLLS9tFdp8T+ttJmHz5KWpfD+IoVF8X+lAtqXoHEgQ5hMlDiE566FqsEJAFFAqpABp1ZZDxW+2HmbLNIuQQrJYacTRQz4IhJAff6XSFDxnMRqhnO334mbFZlqLG7lJKgQgQ9fuIcWvhWpmLpW6RpHtMMjTSW8OIWwqm8LY0bzrjBj0Hb+qu5dPbCwPzvcBeI7tpMTkZtyNANloFODigrkYbCAtqP7EXwMhtX9Fex6DOOn+Xab/59EF+06fJT/l+MHlUOxrxgdXNyO2imDXT7IEZ/jgqg9S3WsgizNNAWsVJWLAlqkFdCLXyXKDw27Qpgz7QzKmYtnMcUg36pBy1uvPusVM9yZtrkkj+A195tsBnrm9No0af0gKMv1NFDVrSu/VGtp1dwEaEWBs6bLiqy5CZ+I2sAPqhm4x713QGXSnpVTir/TCkuyANEsVv8P8JGTX215ef6VMpVWCSXIRq1SB0usbXcAumh69++rCD1xNu2THYOaxF3DjXbLT7yreyrwclpVhnm14RSn/rAbvY3uRrgtFk8p+BsDQNKXymCl//W9+oc8LB+7fJF61GK7YcXry11rSaihX/f56gcmowOOjY89EYIMe7OVo05BxMwj3qiuVUbeP3A6mMCAwEAAQ==";
//        String privateKeyString = "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";
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setIp("127.0.0.1");
        requestInfo.setUrl("/test/1");
        requestInfo.setHttpMethod("post");
        String s = JSONObject.toJSONString(requestInfo);
        String sign = signByPrivateKey(s+"234325", privateKeyString);
        boolean b = verifySignByPublicKey(s, sign, publicKeyString);
        System.out.println(b);


    }
}
