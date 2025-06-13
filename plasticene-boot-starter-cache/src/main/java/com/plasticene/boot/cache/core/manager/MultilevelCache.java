package com.plasticene.boot.cache.core.manager;

import com.plasticene.boot.cache.core.listener.CacheMessage;
import com.plasticene.boot.cache.core.prop.MultilevelCacheProperties;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.executor.PlasticeneThreadExecutor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.concurrent.*;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/20 17:03
 */
@Slf4j
public class MultilevelCache extends AbstractValueAdaptingCache {

    @Resource
    private MultilevelCacheProperties multilevelCacheProperties;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RedissonClient redissonClient;


    ExecutorService cacheExecutor = new PlasticeneThreadExecutor(
            Runtime.getRuntime().availableProcessors() * 2,
            Runtime.getRuntime().availableProcessors() * 20,
            Runtime.getRuntime().availableProcessors() * 200,
           "multilevel-cache-pool"
    );

    private final RedisCache redisCache;
    private final CaffeineCache caffeineCache;

    public MultilevelCache(boolean allowNullValues, RedisCache redisCache, CaffeineCache caffeineCache) {
        super(allowNullValues);
        this.redisCache = redisCache;
        this.caffeineCache = caffeineCache;
    }


    @Override
    @NonNull
    public String getName() {
        return multilevelCacheProperties.getName();
    }

    @Override
    @NonNull
    public Object getNativeCache() {
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
        Object value = lookup(key);
        // 存在就返回
        if (value != null) {
            return (T)(value);
        }
        // 不存在，通过回调写入缓存并返回，注意要求并发情况下回调只能执行一次，保证缓存数据一致性
        // 使用分布式锁
        RLock lock = redissonClient.getLock(key.toString());
        try {
            lock.lock();
            // 双重检测：并发，锁等待，得到锁之后，前面已经数据写入缓存了，这里lookup一定获取值，由此保证回调仅执行一次
            value = lookup(key);
            if (value != null) {
                return (T)(value);
            }
            // 回调生成value
            value = valueLoader.call();
            // 写入缓存
            put(key, value);
            return (T)(value);
        } catch (Exception e) {
            log.error("error:", e);
            throw new BizException("get cache error");
        } finally {
            lock.unlock();
        }
    }

    /**
     * 多级缓存核心逻辑
     * @param key 键
     * @return 值
     */
    @Override
    protected Object lookup(@NonNull Object key) {
        Assert.notNull(key, "key不可为空");
        ValueWrapper value;
        if (multilevelCacheProperties.getCaffeineSwitch()) {
            // 开启一级缓存，先从一级缓存缓存数据
            value = caffeineCache.get(key);
            if (Objects.nonNull(value)) {
                log.info("查询caffeine 一级缓存 key:{}, 返回值是:{}", key, value.get());
                return value.get();
            }
        }
        value = redisCache.get(key);
        if (Objects.nonNull(value)) {
            log.info("查询redis 二级缓存 key:{}, 返回值是:{}", key, value.get());
            // 异步将二级缓存redis写到一级缓存caffeine
            if (multilevelCacheProperties.getCaffeineSwitch()) {
                ValueWrapper finalValue = value;
                cacheExecutor.execute(()-> caffeineCache.put(key, finalValue.get()));
            }
            return value.get();
        }
        return null;
    }

    /**
     *  注意：redis缓存的对象object必须序列化 implements Serializable, 不然缓存对象不成功。
     *  注意：这里asyncPublish()方法是异步发布消息，然后让分布式其他节点清除本地缓存,防止当前节点因更新覆盖数据而其他节点本地缓存保存是脏数据
     *  这样本地缓存数据才能成功存入
     * @param key 键
     * @param value  值
     */
    @Override
    public void put(@NonNull Object key, Object value) {
        redisCache.put(key, value);
        // 异步清除本地缓存
        if (multilevelCacheProperties.getCaffeineSwitch()) {
            asyncPublish(key, value);
        }
    }

    /**
     * key不存在时再保存，存在返回当前值不覆盖
     */
    @Override
    public ValueWrapper putIfAbsent(@NonNull Object key, Object value) {
        ValueWrapper valueWrapper = redisCache.putIfAbsent(key, value);
        // 异步清除本地缓存
        if (multilevelCacheProperties.getCaffeineSwitch()) {
            asyncPublish(key, value);
        }
        return valueWrapper;
    }


    /**
     * 先清除redis中缓存数据，然后通过消息推送清除所有节点caffeine中的缓存
     * 避免短时间内如果先清除caffeine缓存后其他请求会再从redis里加载到caffeine中
     * @param key 键
     */
    @Override
    public void evict(@NonNull Object key) {
        redisCache.evict(key);
        // 异步清除本地缓存
        if (multilevelCacheProperties.getCaffeineSwitch()) {
            asyncPublish(key, null);
        }
    }

    /**
     * 清除所有键
     */
    @Override
    public void clear() {
        redisCache.clear();
        // 异步清除本地缓存
        if (multilevelCacheProperties.getCaffeineSwitch()) {
            asyncPublish(null, null);
        }
    }

    /**
     * 缓存变更时通知其他节点清理本地缓存
     * 异步通过发布订阅主题消息，其他节点监听到之后进行相关本地缓存操作，防止本地缓存脏数据
     */
    void asyncPublish(Object key, Object value) {
        cacheExecutor.execute(()->{
            CacheMessage cacheMessage = new CacheMessage();
            cacheMessage.setCacheName(multilevelCacheProperties.getName());
            cacheMessage.setKey(key);
            cacheMessage.setValue(value);
            redisTemplate.convertAndSend(cacheMessage.getChannel(), cacheMessage);
        });
    }



}
