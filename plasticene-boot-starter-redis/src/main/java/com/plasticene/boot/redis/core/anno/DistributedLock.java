package com.plasticene.boot.redis.core.anno;

import com.plasticene.boot.redis.core.enums.LockType;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/19 10:38
 */

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 分区名字，按照业务类型区分，比如用户服务→user，商品服务→product
     * 防止key重复导致不同业务功能获取同一把分布式锁
     */
    String name() default "";
    /**
     * 锁的key
     */
    String key();
    /**
     * 获取锁的最大尝试时间(单位 {@code unit})
     * 该值大于0则使用 locker.tryLock 方法加锁，否则使用 locker.lock 方法
     */
    long waitTime() default 0;
    /**
     * 加锁的时间(单位 {@code unit})，超过这个时间后锁便自动解锁；
     * 如果leaseTime为-1，则保持锁定直到显式解锁
     */
    long leaseTime() default -1;
    /**
     * 参数的时间单位
     */
    TimeUnit unit() default TimeUnit.SECONDS;


    /**
     * 锁类型，默认为可重入锁
     * @return
     */
    LockType lockType() default LockType.REENTRANT;

}
