package com.plasticene.boot.mybatis.core.encrypt;

import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.mybatis.core.prop.EncryptProperties;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/22 15:56
 */
@Slf4j
public class AESEncryptService implements EncryptService{
    @Resource
    private EncryptProperties encryptProperties;
    @Override
    public String encrypt(String content) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(encryptProperties.getKey().getBytes(StandardCharsets.UTF_8), Constants.AES);
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec secretKeySpec = new SecretKeySpec(enCodeFormat, Constants.AES);
            IvParameterSpec iv = new IvParameterSpec(encryptProperties.getIv().getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance(Constants.AES_CBC_CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv);
            byte[] valueByte = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(valueByte);
        } catch (Exception e) {
            log.error("加密失败：", e);
            throw new BizException("加密失败");
        }
    }

    @Override
    public String decrypt(String content) {
        try {
            byte[] originalData = Base64.getDecoder().decode(content.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec secretKey = new SecretKeySpec(encryptProperties.getKey().getBytes(StandardCharsets.UTF_8), Constants.AES);
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec secretKeySpec = new SecretKeySpec(enCodeFormat, Constants.AES);
            IvParameterSpec iv = new IvParameterSpec(encryptProperties.getIv().getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance(Constants.AES_CBC_CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, iv);
            byte[] valueByte = cipher.doFinal(originalData);
            return new String(valueByte);
        } catch (Exception e) {
            log.error("解密失败：", e);
            throw new BizException("解密失败");
        }
    }
}
