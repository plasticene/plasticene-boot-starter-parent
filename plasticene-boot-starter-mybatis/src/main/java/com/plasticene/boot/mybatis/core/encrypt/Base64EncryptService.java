package com.plasticene.boot.mybatis.core.encrypt;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/22 14:46
 */
public class Base64EncryptService implements EncryptService {
    @Override
    public String encrypt(String content) {
        try {
            return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("encrypt fail!", e);
        }
    }

    @Override
    public String decrypt(String content) {
        try {
            byte[] asBytes = Base64.getDecoder().decode(content);
            return new String(asBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("decrypt fail!", e);
        }
    }
}
