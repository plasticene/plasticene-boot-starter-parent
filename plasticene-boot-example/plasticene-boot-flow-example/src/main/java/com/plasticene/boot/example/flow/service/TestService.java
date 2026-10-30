package com.plasticene.boot.example.flow.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author ZFJ
 * @since 2026/10/29
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TestService {
    private final FlowTaskService flowTaskService;
    private final RedissonClient redissonClient;

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void updateTaskStatus(Long taskId) {
        // 先查询任务状态是否满足
        FlowTask task = flowTaskService.getById(taskId);
        if (!Objects.equals(task.getStatus(), FlowTaskStatusEnum.RUNNING.getCode())) {
            return;
        }
        log.info("加锁之前的查询任务信息: {}", task);
        // 使用Redisson进行分布式锁
        RLock rLock = redissonClient.getLock("lock:" + taskId);
        try {
            // 尝试获取锁
            rLock.lock();
            TimeUnit.SECONDS.sleep(10);
            FlowTask flowTask = flowTaskService.getById(taskId);
            log.info("获取到锁之后的查询任务信息: {}", flowTask);
            FlowTask updateTask = new FlowTask();
            updateTask.setId(taskId);
            updateTask.setStatus(FlowTaskStatusEnum.REJECT.getCode());
            LambdaUpdateWrapper<FlowTask> wrapper = new LambdaUpdateWrapper<FlowTask>()
                    .eq(FlowTask::getId, taskId)
                    .eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode());
            boolean updated = flowTaskService.update(updateTask, wrapper);
            log.info("更新任务状态结果: {}", updated);
        } catch (Exception e) {
            log.error("update task status error: ", e);
        } finally {
            rLock.unlock();
        }

    }
}
