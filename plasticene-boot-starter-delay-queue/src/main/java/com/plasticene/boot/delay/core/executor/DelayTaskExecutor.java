package com.plasticene.boot.delay.core.executor;

import com.plasticene.boot.delay.core.task.DelayTask;

/**
 * 延时任务处理器
 * @author ZFJ
 * @date 2025/10/17
 */
public interface DelayTaskExecutor {
    /**
     * 执行业务任务处理逻辑
     * @param delayedTask 任务
     */
    void run(DelayTask delayedTask);

    /**
     * 不同业务区分不同队列
     * @return 业务队列名称
     */
    String queueName();
}
