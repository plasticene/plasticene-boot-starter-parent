package com.plasticene.boot.web.core.advice;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.plasticene.boot.web.core.prop.ApiSecurityProperties;
import jakarta.annotation.Resource;

import java.util.concurrent.TimeUnit;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public class LocalNonceChecker implements NonceChecker {
    @Resource
    private ApiSecurityProperties apiSecurityProperties;

    private final Cache<String, Boolean> nonceCache;

    public LocalNonceChecker() {
        this.nonceCache = Caffeine.newBuilder()
                .expireAfterWrite(apiSecurityProperties.getValidTime(), TimeUnit.SECONDS)
                .build();
    }

    @Override
    public boolean checkAndStoreNonce(String nonce) {
        // 如果已存在则返回true，否则存储并返回false
        return nonceCache.asMap().putIfAbsent(nonce, Boolean.TRUE) != null;
    }
}
