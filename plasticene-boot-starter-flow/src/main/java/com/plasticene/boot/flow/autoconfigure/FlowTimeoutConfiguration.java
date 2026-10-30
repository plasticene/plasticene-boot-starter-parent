package com.plasticene.boot.flow.autoconfigure;

import com.plasticene.boot.flow.core.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.dao.FlowTimeoutNoticeDAO;
import com.plasticene.boot.flow.core.provider.FlowTimeoutNotification;
import com.plasticene.boot.flow.core.schedule.FlowTimeoutScheduler;
import com.plasticene.boot.flow.core.schedule.ReminderScheduler;
import com.plasticene.boot.flow.core.service.FlowTimeoutService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 注册可关闭的超时扫描器及专用调度线程池。
 * @author ZFJ
 * @since 2026-10-22
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "ptc.flow.timeout", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FlowTimeoutConfiguration {
    @Bean
    @ConditionalOnMissingBean(name = "flowTimeoutSchedulerExecutor")
    ThreadPoolTaskScheduler flowTimeoutSchedulerExecutor() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("flow-timeout-");
        return scheduler;
    }

    @Bean
    FlowTimeoutScheduler flowTimeoutScheduler(FlowTaskDAO flowTaskDAO, FlowTimeoutService flowTimeoutService) {
        return new FlowTimeoutScheduler(flowTaskDAO, flowTimeoutService);
    }

    /**
     * 实现了消息通知，才注入提醒调度器
     */
    @Bean
    @ConditionalOnBean(FlowTimeoutNotification.class)
    ReminderScheduler reminderScheduler(FlowTimeoutService flowTimeoutService) {
        return new ReminderScheduler(flowTimeoutService);
    }
}
