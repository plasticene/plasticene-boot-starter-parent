package com.plasticene.boot.cache.core.manager;

import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/20 17:03
 */
public class CustomRedisCache extends RedisCache {

    /**
     * redisCache的构造方法是protected，外部不能调用，所以通过该类来new redisCache
     * @param name
     * @param cacheWriter
     * @param cacheConfig
     */
    public CustomRedisCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfig) {
        super(name, cacheWriter, cacheConfig);
    }
}