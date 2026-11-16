package com.plasticene.boot.flow.autoconfigure;

import com.plasticene.boot.flow.core.config.FlowTimeoutProperties;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.api.spi.FlowTimeoutNotification;
import com.plasticene.boot.flow.core.impl.timeout.FlowTimeoutScheduler;
import com.plasticene.boot.flow.core.impl.timeout.ReminderScheduler;
import com.plasticene.boot.flow.core.impl.service.timeout.FlowTimeoutService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 集中装配超时任务、专属线程池及其启停生命周期。
 * @author ZFJ
 * @since 2026-11-16
 */
@AutoConfiguration(after = FlowAutoConfiguration.class)
@EnableConfigurationProperties(FlowTimeoutProperties.class)
@ConditionalOnProperty(prefix = "ptc.flow.timeout", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FlowTimeoutConfiguration {
    @Bean
    @ConditionalOnMissingBean(name = "flowTimeoutSchedulerExecutor")
    public ThreadPoolTaskScheduler flowTimeoutSchedulerExecutor(FlowTimeoutProperties properties) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(properties.getPoolSize());
        scheduler.setThreadNamePrefix("flow-timeout-");
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }

    @Bean
    public FlowTimeoutScheduler flowTimeoutScheduler(FlowTaskDAO dao, FlowTimeoutService service) {
        return new FlowTimeoutScheduler(dao, service);
    }

    @Bean
    public SmartLifecycle flowTimeoutLifecycle(FlowTimeoutScheduler scanner, FlowTimeoutService service,
            @org.springframework.beans.factory.annotation.Qualifier("flowTimeoutSchedulerExecutor") ThreadPoolTaskScheduler scheduler,
            ObjectProvider<FlowTimeoutNotification> notifications, FlowTimeoutProperties properties) {
        return new SmartLifecycle() {
            private final List<ScheduledFuture<?>> tasks = new ArrayList<>();
            private boolean running;
            @Override
            public synchronized void start() {
                if (running) { return; }
                tasks.add(scheduler.scheduleWithFixedDelay(scanner::handleDueTasks, Duration.ofMillis(properties.getScanIntervalMs())));
                if (notifications.getIfAvailable() != null) {
                    ReminderScheduler reminders = new ReminderScheduler(service);
                    tasks.add(scheduler.scheduleWithFixedDelay(reminders::sendNotification, Duration.ofMillis(properties.getNoticeScanIntervalMs())));
                }
                running = true;
            }
            @Override
            public synchronized void stop() {
                tasks.forEach(task -> task.cancel(false));
                tasks.clear();
                running = false;
            }
            @Override
            public synchronized boolean isRunning() { return running; }
        };
    }
}
