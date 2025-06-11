package com.plasticene.boot.redis.autoconfigure;

import com.plasticene.boot.redis.core.aop.DistributedLockAspect;
import com.plasticene.boot.redis.core.aop.RateLimitAspect;
import com.plasticene.boot.redis.core.listener.AbstractChannelMessageListener;
import com.plasticene.boot.redis.core.utils.RedisUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/18 14:13
 */
@EnableConfigurationProperties({RedisProperties.class, CacheProperties.class})
@Configuration
@EnableCaching
@Slf4j
public class PlasticeneRedisAutoConfiguration {

    private static final String REDISSON_PREFIX = "redis://";

    @Resource
    private RedisProperties redisProperties;


    /**
     *  注入一个redisTemplate bean，默认JdkSerializationRedisSerializer序列化，存储二进制格式(在客户端不可直观查看)
     *  所以这里改用用json序列化value，string字符串序列化key，这样在客户端控制台ui可直观查看
     *  如果需要和其他语言(如python)共享redis数据，尽量使用{@link StringRedisTemplate}操作
     *  防止两种语言因为序列化不同导致问题，字符串序列化是语言共通的
     */
    @Bean
    @ConditionalOnMissingBean({RedisTemplate.class})
    public  RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setDefaultSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        return template;
    }

    /**
     * 注意一个StringRedisTemplate bean，平时常用
     */
    @Bean
    @ConditionalOnMissingBean({StringRedisTemplate.class})
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    /**
     * 这里默认情况下条件装配注入一个单机模式的redisson client
     * 如需要其他模式，可在业务侧按要求自行注入redissonClient即可
     */
    @Bean
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() {
        // 创建配置
        Config config = new Config();
        String host = redisProperties.getHost();
        int port = redisProperties.getPort();
        config.useSingleServer().setAddress(REDISSON_PREFIX + host + ":" + port)
                .setDatabase(redisProperties.getDatabase())
                .setPassword(redisProperties.getPassword());
        // 根据Config创建出 RedissonClient 实例
        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    public DistributedLockAspect lockAspect() {
        return new DistributedLockAspect();
    }


    @Bean
    @ConditionalOnProperty(name = "ptc.limit.enable", havingValue = "true", matchIfMissing = true)
    public RateLimitAspect rateLimitAspect() {
        return new RateLimitAspect();
    }

    /**
     * 在spring boot 3.x 版本中，StringRedisTemplate被更明确地识别为 RedisTemplate<String, String>
     * 导致容器中存在两个 RedisTemplate 类型的 bean
     * 所以需要使用@Qualifier
     */
    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    public RedisUtils redisUtils(@Qualifier("redisTemplate") RedisTemplate redisTemplate) {
        return new RedisUtils(redisTemplate);
    }

    /**
     * spring redis cache 配置
     */
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration(CacheProperties cacheProperties) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
        // config = config.entryTtl();
        config = config.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));
        config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        CacheProperties.Redis redisProperties = cacheProperties.getRedis();
        //将配置文件中所有的配置都生效
        if (redisProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisProperties.getTimeToLive());
        }
        if (redisProperties.getKeyPrefix() != null) {
            config = config.prefixCacheNameWith(redisProperties.getKeyPrefix());
        }
        if (!redisProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        if (!redisProperties.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }
        return config;
    }


    /**
     * 创建 Redis Pub/Sub 广播消费的容器
     * 统一在这里注入监听器容器，解决多处注入导致冲突问题
     * 我们只需要在业务侧注入监听器bean即可，这里要求监听器bean extends {@link AbstractChannelMessageListener}
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory,
                                                                       List<AbstractChannelMessageListener<?>> listeners) {
        // 创建 RedisMessageListenerContainer 对象
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        // 设置 RedisConnection 工厂。
        container.setConnectionFactory(redisConnectionFactory);
        // 添加监听器
        listeners.forEach(listener -> {
            container.addMessageListener(listener, new ChannelTopic(listener.getChannel()));
            log.info("[redisMessageListenerContainer][注册 Channel({}) 对应的监听器({})]",
                    listener.getChannel(), listener.getClass().getName());
        });
        return container;
    }

}
