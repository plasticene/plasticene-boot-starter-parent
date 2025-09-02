package com.plasticene.boot.web.core.advice;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface NonceChecker {
    /**
     * 校验请求标识nonce是否已存在
     * 存在返回true，不存在返回false并存储
     */
    boolean checkAndStoreNonce(String nonce);
}
