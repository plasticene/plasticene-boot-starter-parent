package com.plasticene.boot.delay.core.task;

import lombok.Data;
import lombok.NonNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 延时任务封装定义
 * @author ZFJ
 * @date 2025/10/16
 */
@Data
public class DelayTask implements Delayed {

    /**
     * 队列名称，区分不同的业务类型
     */
    private final String queueName;
    /**
     * 延时任务id，执行任务时通过id获取任务详情
     */
    private final String taskId;
    /**
     * 任务执行时间  单位：ms
     */
    private final long executeTime;


    public DelayTask(String queueName, String taskId, long executeTime) {
        this.queueName = queueName;
        this.taskId = taskId;
        this.executeTime = executeTime;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(executeTime - System.currentTimeMillis(),
                TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NonNull Delayed o) {
        return Long.compare(executeTime, ((DelayTask)o).executeTime);
    }

    /**
     * taskId可能重复，因为不同的业务队列，任务id可能是相同的
     * @return 唯一的业务任务id
     */
    public String queueTaskId() {
        return this.queueName + ":" + this.taskId;
    }
}
