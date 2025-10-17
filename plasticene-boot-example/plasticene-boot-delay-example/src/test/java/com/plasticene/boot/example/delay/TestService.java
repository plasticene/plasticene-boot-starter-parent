package com.plasticene.boot.example.delay;

import com.plasticene.boot.delay.core.DistributedDelayQueue;
import com.plasticene.boot.delay.core.task.DelayTask;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author ZFJ
 * @date 2025/10/17
 */
@SpringBootTest
public class TestService {
    @Resource
    private DistributedDelayQueue distributedDelayQueue;

    @Test
    public void test() {
        DelayTask task = new DelayTask("order", "001", System.currentTimeMillis() + 3*60*1000);
        distributedDelayQueue.addTask(task);
        DelayTask task2 = new DelayTask("order", "002", System.currentTimeMillis() + 15*60*1000);
        distributedDelayQueue.addTask(task2);
    }
}
