package com.plasticene.boot.example.delay.Controller;

import cn.hutool.core.collection.CollUtil;
import com.plasticene.boot.delay.core.DistributedDelayQueue;
import com.plasticene.boot.delay.core.task.DelayTask;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * @author ZFJ
 * @date 2025/10/17
 */
@RestController
@RequestMapping("/delay/test")
public class DelayController {
    @Resource
    private DistributedDelayQueue distributedDelayQueue;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/add")
    @Operation(summary = "添加延时任务")
    public void addDelayTask() {
        DelayTask task = new DelayTask("order", "001", System.currentTimeMillis() + 3*60*1000);
        distributedDelayQueue.addTask(task);
        DelayTask task2 = new DelayTask("order", "002", System.currentTimeMillis() + 15*60*1000);
        distributedDelayQueue.addTask(task2);
        DelayTask task3 = new DelayTask("order", "003", System.currentTimeMillis() + 20*60*1000);
        distributedDelayQueue.addTask(task3);
        DelayTask task4 = new DelayTask("order", "004", System.currentTimeMillis() + 12*60*1000);
        distributedDelayQueue.addTask(task4);
        DelayTask task5 = new DelayTask("order", "005", System.currentTimeMillis() + 6*60*1000);
        distributedDelayQueue.addTask(task5);
    }

    // 每分钟扫描一次延时任务
    @Scheduled(cron = "0 * * * * *")
    public void scheduleScan() {
        // 读取时间到了的延时任务
        Set<String> taskIds = stringRedisTemplate.opsForZSet().rangeByScore("task-key", 0, System.currentTimeMillis());
        if (CollUtil.isEmpty(taskIds)) {
            return;
        }
        taskIds.forEach(taskId -> {
            // 处理延时任务
        });
        // 最后删除任务
    }
}
