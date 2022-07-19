package com.plasticene.boot.redis.autoconfigure;

import com.plasticene.boot.redis.core.aop.LockAspect;
import com.plasticene.boot.redis.core.aop.RateLimitAspect;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.Resource;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/18 14:13
 */
@EnableConfigurationProperties({RedisProperties.class})
@Configuration
public class PlasticeneRedisAutoConfiguration {

    private static final String REDISSON_PREFIX = "redis://";

    @Resource
    private RedisProperties redisProperties;


    /**
     *  注入一个redisTemplate bean，使用json序列化value
     * @param factory
     * @return
     */
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
    @ConditionalOnMissingBean({StringRedisTemplate.class})
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    /**
     * 这里默认情况下条件装配注入一个单机模式的redisson client
     * 如需要其他模式，可在业务侧按要求自行注入redissonClient覆盖即可
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() {
        // 1、创建配置
        Config config = new Config();
        String host = redisProperties.getHost();
        int port = redisProperties.getPort();
        config.useSingleServer().setAddress(REDISSON_PREFIX + host + ":" + port)
                .setDatabase(redisProperties.getDatabase())
                .setPassword(redisProperties.getPassword());
        // 2、根据 Config 创建出 RedissonClient 实例
        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    public LockAspect lockAspect() {
        return new LockAspect();
    }


    @Bean
    @ConditionalOnProperty(name = "ptc.limit.enable", havingValue = "true", matchIfMissing = true)
    public RateLimitAspect rateLimitAspect() {
        return new RateLimitAspect();
    }
}
