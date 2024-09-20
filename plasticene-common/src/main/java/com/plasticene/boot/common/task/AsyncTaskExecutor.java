package com.plasticene.boot.common.task;

import com.plasticene.boot.common.executor.PlasticeneThreadExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2024/9/20 11:38
 */
@Slf4j
public class AsyncTaskExecutor {

    private final static PlasticeneThreadExecutor DEFAULT_THREAD_EXECUTOR
            = new PlasticeneThreadExecutor(Runtime.getRuntime().availableProcessors() + 1,
            Runtime.getRuntime().availableProcessors() * 20,
            1000,
            "defaultAsyncTaskExecutor-"
            );


    /**
     * 异步任务执行
     * @param input 入参
     * @param task 回调对象
     * @param threadExecutor  线程池
     * @param <T> 入参泛型
     * @param <R> 出参泛型
     * @return
     */
    public static <T, R> CompletableFuture<R> submitTask(T input,
                                                         CallbackTask<T, R> task,
                                                         PlasticeneThreadExecutor threadExecutor) {

        // 异步执行
        return CompletableFuture.supplyAsync(() -> {
            // 执行任务
            return task.execute(input);
        }, threadExecutor).thenApply(result -> {
            // 任务成功后执行onSuccess回调
            log.info("任务执行成功回调======>>>>result: {}", result);
            task.onSuccess(result);
            return result;
        }).exceptionally(throwable -> {
            // 任务失败后执行onFailure回调
            log.error("任务执行失败回调======>>>>error:", throwable);
            task.onFailure(throwable);
            // 失败时返回默认值null
            return null;
        });
    }

    /**
     * 异步任务执行 使用默认线程池
     */
    public static <T, R> CompletableFuture<R> submitTask(T input, CallbackTask<T, R> task) {
        return submitTask(input, task, DEFAULT_THREAD_EXECUTOR);
    }

    /**
     * 异步任务执行 入参为Void 返回结果为R
     */
    public static <Void, R> CompletableFuture<R> submitTask(CallbackTask<Void, R> task,
                                                         PlasticeneThreadExecutor threadExecutor) {
        return submitTask(null, task, threadExecutor);
    }

    /**
     * 异步任务执行 入参为Void 返回结果为R  使用默认线程池
     */
    public static <Void, R> CompletableFuture<R> submitTask(CallbackTask<Void, R> task) {
        return submitTask(null, task, DEFAULT_THREAD_EXECUTOR);
    }







    public static void main(String[] args) {
        CallbackTask<String, Integer> callbackTask = new CallbackTask<String, Integer>() {
            @Override
            public Integer execute(String s) {
                log.info("任务执行ing");
                char c = s.charAt(15);
                return s.length();
            }

            @Override
            public void onSuccess(Integer result) {
                log.info("任务执行成功======>>>>result: " + result);
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("任务执行失败======>>>>error: ", throwable);
            }
        };
        List<String> list = new ArrayList<>();
        list.add("hello");
        list.add("world");
        list.add("student");
        list.forEach(s -> AsyncTaskExecutor.submitTask(s, callbackTask));


    }
}
