package com.plasticene.boot.cache.core.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/20 16:27
 */
@ConfigurationProperties(prefix = "multilevel.cache")
@Data
public class MultilevelCacheProperties {

    /**
     * 一级本地缓存最大比例
     */
    private Double maxCapacityRate = 0.2;

    /**
     * 一级本地缓存与最大缓存初始化大小比例
     */
    private Double initRate = 0.5;

    /**
     * 消息主题
     */
//    private String topic = "multilevel-cache-topic";

    /**
     * 缓存名称
     */
    private String name = "multilevel-cache";

    /**
     * 一级本地缓存名称
     */
    private String caffeineName = "multilevel-caffeine-cache";

    /**
     * 二级缓存名称
     */
    private String redisName = "multilevel-redis-cache";

    /**
     * 一级本地缓存过期时间
     */
    private Integer caffeineExpireTime = 300;

    /**
     * 二级缓存过期时间
     */
    private Integer redisExpireTime = 600;


    /**
     * 一级缓存开关
     */
    private Boolean caffeineSwitch = true;





}

