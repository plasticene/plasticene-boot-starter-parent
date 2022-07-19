package com.plasticene.boot.redis.core.enums;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/19 10:47
 */
public enum LockType {
    //可重入锁
    REENTRANT,
    //公平锁
    FAIR,
    //读锁
    READ,
    //写锁
    WRITE
}
