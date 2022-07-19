package com.plasticene.boot.redis.core.aop;

import cn.hutool.core.util.StrUtil;
import com.plasticene.boot.common.aspect.AbstractAspectSupport;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.redis.core.anno.DistributedLock;
import com.plasticene.boot.redis.core.enums.LockType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/19 10:59
 */
@Aspect
public class LockAspect extends AbstractAspectSupport {

    private static final Logger logger = LoggerFactory.getLogger(LockAspect.class);

    @Resource
    private RedissonClient redissonClient;

    // 指定切入点为DistributedLock注解
    @Pointcut("@annotation(com.plasticene.boot.redis.core.anno.DistributedLock)")
    public void distributedLockAnnotationPointcut() {
    }

    // 环绕通知
    @Around("distributedLockAnnotationPointcut()")
    public Object aroundLock(ProceedingJoinPoint pjp) {
        Method method = resolveMethod(pjp);
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);
        String lockKey = distributedLock.key();
        if (StrUtil.isBlank(lockKey)) {
            throw new BizException("lock Key 不能为空");
        }
        // 加锁等待时间 大于0就调用redisson的tryLock()
        long waitTime = distributedLock.waitTime();
        // 锁自动过期时间
        long leaseTime = distributedLock.leaseTime();
        TimeUnit unit = distributedLock.unit();
        // 锁类型
        LockType lockType = distributedLock.lockType();

        RLock rLock = getLock(lockType, lockKey);
        boolean flag = true;
        try {
            if (waitTime > 0) {
                flag = rLock.tryLock(waitTime, leaseTime, unit);
            } else {
                rLock.lock(leaseTime, unit);
            }
            if (flag) {
                return pjp.proceed();
            } else {
                throw new BizException("获取锁等待超时");
            }
        } catch (Throwable e) {
            logger.error("加锁异常：", e);
            if (e instanceof BizException) {
                throw new BizException(e.getMessage());
            }
            throw new BizException("加锁失败");
        } finally {
            if (flag) {
                rLock.unlock();
            }
        }
    }

    RLock getLock(LockType lockType, String lockKey) {
        RLock rLock = null;
        switch (lockType) {
            case FAIR:
                rLock = redissonClient.getFairLock(lockKey);
                break;
            case REENTRANT:
                rLock = redissonClient.getLock(lockKey);
                break;
            case READ:
                RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
                rLock = readWriteLock.readLock();
                break;
            case WRITE:
                RReadWriteLock rwLock = redissonClient.getReadWriteLock(lockKey);
                rLock = rwLock.writeLock();
                break;
            default:
                rLock = redissonClient.getLock(lockKey);
        }
        return rLock;

    }

}
