package com.plasticene.boot.cache.core.listener;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/20 17:12
 */
@Slf4j
public class CaffeineCacheRemovalListener implements RemovalListener<Object, Object> {
    @Override
    public void onRemoval(@Nullable Object k, @Nullable Object v, @NonNull RemovalCause cause) {
        log.info("[移除缓存] key:{} reason:{}", k, cause.name());
        // 超出最大缓存
        if (cause == RemovalCause.SIZE) {

        }
        // 超出过期时间
        if (cause == RemovalCause.EXPIRED) {
            // do something
        }
        // 显式移除
        if (cause == RemovalCause.EXPLICIT) {
            // do something
        }
        // 旧数据被更新
        if (cause == RemovalCause.REPLACED) {
            // do something
        }
    }
}
