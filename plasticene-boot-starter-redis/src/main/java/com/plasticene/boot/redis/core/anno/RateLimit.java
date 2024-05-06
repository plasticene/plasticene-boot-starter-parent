package com.plasticene.boot.redis.core.anno;

import com.plasticene.boot.redis.core.enums.LimitType;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/18 16:55
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RateLimit {

    /**
     * 分区名字，按照业务类型区分，比如用户服务→user，商品服务→product
     */
    String name() default "";

    /**
     * key
     */
    String key() default "";

    /**
     * 给定的时间范围 默认单位(秒)
     */
    int period();

    /**
     * Key的前缀
     */
    String prefix() default "";

    /**
     * 时间单位
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * 一定时间内最多访问次数
     */
    int count();

    /**
     * 限流的类型(用户自定义key 或者 请求ip，默认为自定义key)
     */
    LimitType limitType() default LimitType.CUSTOMER;
}
