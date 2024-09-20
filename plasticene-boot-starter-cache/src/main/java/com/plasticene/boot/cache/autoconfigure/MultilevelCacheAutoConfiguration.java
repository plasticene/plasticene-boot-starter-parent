package com.plasticene.boot.cache.autoconfigure;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.plasticene.boot.cache.core.listener.CaffeineCacheRemovalListener;
import com.plasticene.boot.cache.core.listener.RedisCacheMessageListener;
import com.plasticene.boot.cache.core.manager.CustomRedisCache;
import com.plasticene.boot.cache.core.manager.MultilevelCache;
import com.plasticene.boot.cache.core.prop.MultilevelCacheProperties;
import com.plasticene.boot.common.executor.PlasticeneThreadExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;

import static org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/20 17:24
 */
@Configuration
@EnableConfigurationProperties(MultilevelCacheProperties.class)
public class MultilevelCacheAutoConfiguration {

    @Resource
    private MultilevelCacheProperties multilevelCacheProperties;

    ExecutorService cacheExecutor = new PlasticeneThreadExecutor(
            Runtime.getRuntime().availableProcessors() * 2,
            Runtime.getRuntime().availableProcessors() * 20,
            Runtime.getRuntime().availableProcessors() * 200,
            "cache-pool"
    );

    @Bean
    @ConditionalOnMissingBean({RedisTemplate.class})
    public  RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<Object, Object>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setDefaultSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        return template;
    }

    @Bean
    public RedisCache redisCache (RedisConnectionFactory redisConnectionFactory) {
        RedisCacheWriter redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory);
        RedisCacheConfiguration redisCacheConfiguration = defaultCacheConfig();
        redisCacheConfiguration = redisCacheConfiguration.entryTtl(Duration.of(multilevelCacheProperties.getRedisExpireTime(), ChronoUnit.SECONDS));
        redisCacheConfiguration = redisCacheConfiguration.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));
        redisCacheConfiguration = redisCacheConfiguration.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        RedisCache redisCache = new CustomRedisCache(multilevelCacheProperties.getRedisName(), redisCacheWriter, redisCacheConfiguration);
        return redisCache;
    }

    /**
     * 由于Caffeine 不会再值过期后立即执行清除，而是在写入或者读取操作之后执行少量维护工作，或者在写入读取很少的情况下，偶尔执行清除操作。
     * 如果我们项目写入或者读取频率很高，那么不用担心。如果想入写入和读取操作频率较低，那么我们可以通过Cache.cleanUp()或者加scheduler去定时执行清除操作。
     * Scheduler可以迅速删除过期的元素，***Java 9 +***后的版本，可以通过Scheduler.systemScheduler(), 调用系统线程，达到定期清除的目的
     * @return
     */
    @Bean
    @ConditionalOnClass(CaffeineCache.class)
    @ConditionalOnProperty(name = "multilevel.cache.caffeineSwitch", havingValue = "true", matchIfMissing = true)
    public CaffeineCache caffeineCache() {
        CaffeineCache caffeineCache = new CaffeineCache(multilevelCacheProperties.getCaffeineName(), Caffeine.newBuilder()
                // 设置初始缓存大小
                .initialCapacity(multilevelCacheProperties.getInitCapacity())
                // 设置最大缓存
                .maximumSize(multilevelCacheProperties.getMaxCapacity())
                // 设置缓存线程池
                .executor(cacheExecutor)
                // 设置定时任务执行过期清除操作
//                .scheduler(Scheduler.systemScheduler())
                // 监听器(超出最大缓存)
                .removalListener(new CaffeineCacheRemovalListener())
                // 设置缓存读时间的过期时间
                .expireAfterAccess(Duration.of(multilevelCacheProperties.getCaffeineExpireTime(), ChronoUnit.SECONDS))
                // 开启metrics监控
                .recordStats()
                .build());
        return caffeineCache;
    }

    @Bean
    @ConditionalOnBean({CaffeineCache.class, RedisCache.class})
    public MultilevelCache multilevelCache(RedisCache redisCache, CaffeineCache caffeineCache) {
        MultilevelCache multilevelCache = new MultilevelCache(true, redisCache, caffeineCache);
        return multilevelCache;
    }

    @Bean
    public RedisCacheMessageListener redisCacheMessageListener(@Autowired CaffeineCache caffeineCache) {
        RedisCacheMessageListener redisCacheMessageListener = new RedisCacheMessageListener();
        redisCacheMessageListener.setCaffeineCache(caffeineCache);
        return redisCacheMessageListener;
    }



//    @Bean
//    @ConditionalOnMissingBean({RedisMessageListenerContainer.class})
//    public RedisMessageListenerContainer redisMessageListenerContainer(@Autowired RedisConnectionFactory redisConnectionFactory,
//                                                                       @Autowired RedisCacheMessageListener redisCacheMessageListener) {
//        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
//        redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
//        redisMessageListenerContainer.addMessageListener(redisCacheMessageListener, new ChannelTopic("multilevel-cache-topic"));
//        return redisMessageListenerContainer;
//    }


}
