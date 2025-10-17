package com.plasticene.boot.example.delay.executor;

import com.plasticene.boot.delay.core.executor.DelayTaskExecutor;
import com.plasticene.boot.delay.core.task.DelayTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ZFJ
 * @date 2025/10/17
 */
@Component
@Slf4j
public class OrderTaskExecutor implements DelayTaskExecutor {
    @Override
    public void run(DelayTask delayedTask) {
        log.info("run delay task {}", delayedTask);
    }

    @Override
    public String queueName() {
        return "order";
    }
}
