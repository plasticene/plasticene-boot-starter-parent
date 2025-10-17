package com.plasticene.boot.delay.core.storage;

import cn.hutool.core.collection.CollUtil;
import com.plasticene.boot.delay.core.constant.DelayConstant;
import com.plasticene.boot.delay.core.task.DelayTask;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 基于redis实现任务存储功能
 * @author ZFJ
 * @date 2025/10/17
 */
public class RedisTaskStorage implements TaskStorage {
    @Resource
    private StringRedisTemplate stringRedisTemplate;



    @Override
    public void addTask(DelayTask task) {
        String queueName = task.getQueueName();
        String taskId = task.getTaskId();
        long executeTime = task.getExecuteTime();
        // 不同业务的任务单独存储，防止一个key存储所有任务导致big key
        stringRedisTemplate.opsForZSet().add(DelayConstant.DELAY_TASK_KEY_PREFIX + queueName, taskId, executeTime);

    }

    @Override
    public void removeTask(DelayTask task) {
        String queueName = task.getQueueName();
        String taskId = task.getTaskId();
        stringRedisTemplate.opsForZSet().remove(DelayConstant.DELAY_TASK_KEY_PREFIX + queueName, taskId);
    }

    @Override
    public List<DelayTask> listTask(String queueName, Long startTime, Long endTime) {
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .rangeByScoreWithScores(DelayConstant.DELAY_TASK_KEY_PREFIX + queueName, startTime, endTime);
        List<DelayTask> result = new ArrayList<>();
        if (CollUtil.isEmpty(tuples)) {
            return result;
        }
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String taskId = tuple.getValue();
            Double score = tuple.getScore();
            assert score != null;
            DelayTask task = new DelayTask(queueName, taskId, score.longValue());
            result.add(task);
        }
        return result;
    }

    @Override
    public void addExecutedTask(DelayTask task) {
        String queueTaskId = task.queueTaskId();
        stringRedisTemplate.opsForZSet().add(DelayConstant.DELAY_EXECUTED_KEY, queueTaskId, System.currentTimeMillis());
    }

    @Override
    public boolean isExecuted(DelayTask task) {
        String queueTaskId = task.queueTaskId();
        Double score = stringRedisTemplate.opsForZSet().score(DelayConstant.DELAY_EXECUTED_KEY, queueTaskId);
        if (score == null) {
            return false;
        }
        // 如果任务的执行时间大于记录的执行时间，说明是业务对此任务调整执行时间后重新入队，需要再次执行，并不是框架分配导致的重复执行问题
        long executeTime = task.getExecuteTime();
        return executeTime <= score.longValue();
    }

    @Override
    public void removeExecutedTask(Long startTime, Long endTime) {
        stringRedisTemplate.opsForZSet().removeRangeByScore(DelayConstant.DELAY_EXECUTED_KEY, startTime, endTime);
    }
}
