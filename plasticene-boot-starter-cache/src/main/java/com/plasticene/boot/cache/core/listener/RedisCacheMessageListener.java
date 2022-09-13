package com.plasticene.boot.cache.core.listener;

import com.plasticene.boot.redis.core.listener.AbstractChannelMessageListener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.caffeine.CaffeineCache;


import java.util.Objects;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/20 17:13
 */
@Slf4j
@Data
public class RedisCacheMessageListener extends AbstractChannelMessageListener<CacheMessage> {

    private CaffeineCache caffeineCache;

    @Override
    public void onMessage(CacheMessage message) {
        log.info("监听的redis message: {}" + message.toString());
        if (Objects.isNull(message.getKey())) {
            caffeineCache.invalidate();
        } else {
            caffeineCache.evict(message.getKey());
        }
    }
}
