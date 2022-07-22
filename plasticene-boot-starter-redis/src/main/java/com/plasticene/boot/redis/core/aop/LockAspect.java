package com.plasticene.boot.redis.core.aop;

import cn.hutool.core.util.StrUtil;
import com.plasticene.boot.common.aspect.AbstractAspectSupport;
import com.plasticene.boot.common.constant.OrderConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.redis.core.anno.DistributedLock;
import com.plasticene.boot.redis.core.enums.LockType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/19 10:59
 */
@Aspect
@Order(OrderConstant.AOP_LOCK)
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
        logger.info("锁模式->{},  等待锁定时间->{}秒,   锁定最长时间->{}秒",lockType.name(), waitTime, leaseTime);
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

    /**
     * 可重入锁
     * 阻塞式等待。默认加的锁都是30s
     * 1）、锁的自动续期，如果业务超长，运行期间自动锁上新的30s。不用担心业务时间长，锁自动过期被删掉
     * 2）、加锁的业务只要运行完成，就不会给当前锁续期，即使不手动解锁，锁默认会在30s内自动过期，不会产生死锁问题
     * leaseTime为锁超时时间，leaseTime为-1代表不自动解锁
     * 1）、myLock.lock(10,TimeUnit.SECONDS);  10秒钟自动解锁,自动解锁时间一定要大于业务执行时间
     * 2）、如果没有指定锁的超时时间，就使用 lockWatchdogTimeout = 30 * 1000 【看门狗默认时间】。只要占锁成功，就会启动一个
     *  定时任务【重新给锁设置过期时间，新的过期时间就是看门狗的默认时间】,每隔10秒都会自动的再次续期，续成30秒
     * internalLockLeaseTime 【看门狗时间】 / 3， 10s
     *
     * 读写锁
     * 保证一定能读到最新数据，修改期间，写锁是一个排它锁（互斥锁、独享锁），读锁是一个共享锁
     * 写锁没释放读锁必须等待
     * 读 + 读 ：相当于无锁，并发读，只会在Redis中记录好，所有当前的读锁。他们都会同时加锁成功
     * 写 + 读 ：必须等待写锁释放
     * 写 + 写 ：阻塞方式
     * 读 + 写 ：有读锁。写也需要等待
     * 只要有读或者写的存都必须等待
     * @return
     */

    RLock getLock(LockType lockType, String lockKey) {
        // 获取一把锁，只要锁的名字一样，就是同一把锁
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
