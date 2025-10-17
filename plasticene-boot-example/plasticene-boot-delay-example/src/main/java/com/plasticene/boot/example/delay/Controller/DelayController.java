package com.plasticene.boot.example.delay.Controller;

import com.plasticene.boot.delay.core.DistributedDelayQueue;
import com.plasticene.boot.delay.core.task.DelayTask;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ZFJ
 * @date 2025/10/17
 */
@RestController
@RequestMapping("/delay/test")
public class DelayController {
    @Resource
    private DistributedDelayQueue distributedDelayQueue;

    @PostMapping("/add")
    @Operation(summary = "添加延时任务")
    public void addDelayTask() {
        DelayTask task = new DelayTask("order", "001", System.currentTimeMillis() + 3*60*1000);
        distributedDelayQueue.addTask(task);
        DelayTask task2 = new DelayTask("order", "002", System.currentTimeMillis() + 15*60*1000);
        distributedDelayQueue.addTask(task2);
    }
}
