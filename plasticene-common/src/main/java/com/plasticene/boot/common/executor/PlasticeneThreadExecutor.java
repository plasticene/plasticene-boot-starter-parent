package com.plasticene.boot.common.executor;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.alibaba.ttl.TtlCallable;
import com.alibaba.ttl.TtlRunnable;
import lombok.NonNull;

import java.util.Objects;
import java.util.concurrent.*;

/**
 *
 * 这是Spring ThreadPoolTaskExecutor的一个简单替换，可搭配TransmittableThreadLocal实现父子线程之间的数据传递
 *
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/14 14:49
 */
public class PlasticeneThreadExecutor extends ThreadPoolExecutor {

    public PlasticeneThreadExecutor(int core, int max, int queueCapacity, String name) {
        this(
                core,
                max,
                0L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new ThreadFactoryBuilder().setNamePrefix(name).build(),
                new AbortPolicy()
        );
    }

    public PlasticeneThreadExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }


    @Override
    public void execute(@NonNull Runnable runnable) {
        Runnable ttlRunnable = TtlRunnable.get(runnable);
        Objects.requireNonNull(ttlRunnable, "ttlRunnable is null");
        super.execute(ttlRunnable);
    }

    @Override
    @NonNull
    public <T> Future<T> submit(@NonNull Callable<T> task) {
        Callable<T> ttlCallable = TtlCallable.get(task);
        Objects.requireNonNull(ttlCallable, "ttlRunnable is null");
        return super.submit(ttlCallable);
    }

    @Override
    @NonNull
    public Future<?> submit(@NonNull Runnable task) {
        Runnable ttlRunnable = TtlRunnable.get(task);
        Objects.requireNonNull(ttlRunnable, "ttlRunnable is null");
        return super.submit(ttlRunnable);
    }

    @Override
    @NonNull
    public <T> Future<T> submit(@NonNull Runnable task, T result) {
        Runnable ttlRunnable = TtlRunnable.get(task);
        Objects.requireNonNull(ttlRunnable, "ttlRunnable is null");
        return super.submit(ttlRunnable, result);
    }

}

