package com.plasticene.boot.example.flow;

import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * @author ZFJ
 * @date 2025/11/18
 */
@SpringBootTest
@Slf4j
@Component
public class FlowRuntimeServiceTest {
    @Resource
    private FlowRuntimeService flowRuntimeService;

    @Transactional(rollbackFor = Exception.class)
    public void selectInstanceForUpdate(Long id) {
        log.info("线程：{}, 开始时间：{}", Thread.currentThread().getName(), LocalDateTime.now());
        FlowInstance instance = flowRuntimeService.selectInstanceForUpdate(id);
        try {
            TimeUnit.SECONDS.sleep(3);
            log.info("当前时间: {},  instance: {}", LocalDateTime.now(), instance);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("线程：{}, 结束时间：{}", Thread.currentThread().getName(), LocalDateTime.now());
    }
}
