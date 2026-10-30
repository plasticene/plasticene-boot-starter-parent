package com.plasticene.boot.flow.autoconfigure;

import com.plasticene.boot.flow.core.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.dao.FlowTimeoutNoticeDAO;
import com.plasticene.boot.flow.core.provider.FlowTimeoutNotification;
import com.plasticene.boot.flow.core.schedule.FlowTimeoutScheduler;
import com.plasticene.boot.flow.core.service.FlowTimeoutService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * 验证通知由使用方提供及定时扫描开关。
 * @author ZFJ
 * @since 2026-10-22
 */
class FlowTimeoutConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(FlowTimeoutConfiguration.class)
            .withBean(FlowTaskDAO.class, () -> mock(FlowTaskDAO.class))
            .withBean(FlowTimeoutNoticeDAO.class, () -> mock(FlowTimeoutNoticeDAO.class))
            .withBean(FlowTimeoutService.class, () -> mock(FlowTimeoutService.class));

    @Test
    void shouldLoadSchedulerWithoutProvidingPlaceholderNotification() {
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(context.getBeansOfType(FlowTimeoutNotification.class).isEmpty());
            assertEquals(1, context.getBeansOfType(FlowTimeoutScheduler.class).size());
            var executor = context.getBean("flowTimeoutSchedulerExecutor",
                    org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler.class);
            assertEquals(2, executor.getPoolSize());
            var processor = context.getBean(org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor.class);
            assertEquals(2, processor.getScheduledTasks().size());
        });
    }

    @Test
    void shouldAllowCustomChannelAndDisableScheduler() {
        FlowTimeoutNotification channel = message -> {};
        runner.withPropertyValues("ptc.flow.timeout.enabled=false")
                .withBean(FlowTimeoutNotification.class, () -> channel).run(context -> {
                    assertNull(context.getStartupFailure());
                    assertSame(channel, context.getBean(FlowTimeoutNotification.class));
                    assertTrue(context.getBeansOfType(FlowTimeoutScheduler.class).isEmpty());
                    assertFalse(context.containsBean("flowTimeoutSchedulerExecutor"));
                });
    }
}
