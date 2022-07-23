package com.plasticene.boot.mybatis.core.encrypt;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/22 14:43
 */
public interface EncryptService {

    /**
     * 加密算法
     * @param content
     * @return
     */
    String encrypt(String content);

    /**
     * 解密算法
     * @param content
     * @return
     */
    String decrypt(String content);

}
