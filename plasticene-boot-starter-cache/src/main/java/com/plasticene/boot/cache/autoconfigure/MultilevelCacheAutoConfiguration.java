package com.plasticene.boot.cache.autoconfigure;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.plasticene.boot.cache.core.listener.CaffeineCacheRemovalListener;
import com.plasticene.boot.cache.core.listener.RedisCacheMessageListener;
import com.plasticene.boot.cache.core.manager.CustomRedisCache;
import com.plasticene.boot.cache.core.manager.MultilevelCache;
import com.plasticene.boot.cache.core.prop.MultilevelCacheProperties;
import com.plasticene.boot.common.executor.plasticeneThreadExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
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
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
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

    ExecutorService cacheExecutor = new plasticeneThreadExecutor(
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
     * ??????Caffeine ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????Cache.cleanUp()?????????scheduler??????????????????????????????
     * Scheduler????????????????????????????????????***Java 9 +***???????????????????????????Scheduler.systemScheduler(), ????????????????????????????????????????????????
     * @return
     */
    @Bean
    @ConditionalOnClass(CaffeineCache.class)
    @ConditionalOnProperty(name = "multilevel.cache.caffeineSwitch", havingValue = "true", matchIfMissing = true)
    public CaffeineCache caffeineCache() {
        int maxCapacity = (int) (Runtime.getRuntime().totalMemory() * multilevelCacheProperties.getMaxCapacityRate());
        int initCapacity = (int) (maxCapacity * multilevelCacheProperties.getInitRate());
        CaffeineCache caffeineCache = new CaffeineCache(multilevelCacheProperties.getCaffeineName(), Caffeine.newBuilder()
                // ????????????????????????
                .initialCapacity(initCapacity)
                // ??????????????????
                .maximumSize(maxCapacity)
                // ?????????????????????
                .executor(cacheExecutor)
                // ??????????????????????????????????????????
//                .scheduler(Scheduler.systemScheduler())
                // ?????????(??????????????????)
                .removalListener(new CaffeineCacheRemovalListener())
                // ????????????????????????????????????
                .expireAfterAccess(Duration.of(multilevelCacheProperties.getCaffeineExpireTime(), ChronoUnit.SECONDS))
                // ??????metrics??????
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



    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(@Autowired RedisConnectionFactory redisConnectionFactory,
                                                                       @Autowired RedisCacheMessageListener redisCacheMessageListener) {
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
        redisMessageListenerContainer.addMessageListener(redisCacheMessageListener, new ChannelTopic(multilevelCacheProperties.getTopic()));
        return redisMessageListenerContainer;
    }

}
