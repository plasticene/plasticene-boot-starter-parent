package com.plasticene.boot.delay.core.storage;

import com.plasticene.boot.delay.core.task.DelayTask;

import java.util.List;

/**
 * 任务存储接口
 * @author ZFJ
 * @date 2025/10/16
 */
public interface TaskStorage {
    /**
     * 新增任务
     * @param task 延迟任务
     */
    void addTask(DelayTask task);

    /**
     * 删除任务
     * @param task 延迟任务
     */
    void removeTask(DelayTask task);

    /**
     * 获取任务数据
     */
    List<DelayTask> listTask(String queueName, Long startTime, Long endTime);

    /**
     * 记录已经执行过的任务
     * @param task 延时任务
     */
    void addExecutedTask(DelayTask task);

    /**
     * 判断当前任务是否执行过
     * @param task 延时任务
     * @return 执行过标识
     */
    boolean isExecuted(DelayTask task);

    /**
     * 删除执行过的任务
     */
    void removeExecutedTask(Long startTime, Long endTime);
}
