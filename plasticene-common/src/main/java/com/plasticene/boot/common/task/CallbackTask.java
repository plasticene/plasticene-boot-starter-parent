package com.plasticene.boot.common.task;


/**
 * @author fjzheng
 * @version 1.0
 * @date 2024/9/20 11:15
 * 回调任务接口
 */
@FunctionalInterface
public interface CallbackTask<T, R> {

    /**
     * 执行任务，接收一个参数返回一个结果
     * @param t
     * @return
     */
    R execute(T t);

    /**
     * 任务执行成功回调
     * @param result
     */
    default void onSuccess(R result) {}

    /**
     * 任务执行失败回调
     * @param throwable
     */
    default void onFailure(Throwable throwable) {};
}
