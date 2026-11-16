package com.plasticene.boot.flow.autoconfigure;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.plasticene.boot.flow.core.api.spi.FlowTimeoutNotification;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTask;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTimeoutNotice;
import com.plasticene.boot.flow.core.impl.service.timeout.FlowTimeoutService;
import com.plasticene.boot.flow.core.impl.timeout.FlowTimeoutScheduler;
import com.plasticene.boot.flow.core.impl.timeout.ReminderScheduler;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证通知由使用方提供及定时扫描开关。
 * @author ZFJ
 * @since 2026-10-22
 */
class FlowTimeoutConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(FlowTimeoutConfiguration.class)
            .withBean(FlowTaskDAO.class, this::taskDAO)
            .withBean(FlowTimeoutService.class, this::timeoutService);

    @BeforeAll
    static void initializeEntityMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "timeout-task"), FlowTask.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "timeout-notice"), FlowTimeoutNotice.class);
    }

    @Test
    void shouldLoadSchedulerWithoutProvidingPlaceholderNotification() {
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(context.getBeansOfType(FlowTimeoutNotification.class).isEmpty());
            assertEquals(1, context.getBeansOfType(FlowTimeoutScheduler.class).size());
            assertTrue(context.getBeansOfType(ReminderScheduler.class).isEmpty());
            var executor = context.getBean("flowTimeoutSchedulerExecutor",
                    ThreadPoolTaskScheduler.class);
            assertEquals(2, executor.getScheduledThreadPoolExecutor().getCorePoolSize());
            var processor = context.getBean(ScheduledAnnotationBeanPostProcessor.class);
            assertEquals(1, processor.getScheduledTasks().size());
        });
    }

    @Test
    void shouldRegisterReminderSchedulerWhenNotificationIsProvided() {
        FlowTimeoutNotification channel = message -> {};
        runner.withBean(FlowTimeoutNotification.class, () -> channel).run(context -> {
            assertNull(context.getStartupFailure());
            assertSame(channel, context.getBean(FlowTimeoutNotification.class));
            assertEquals(1, context.getBeansOfType(FlowTimeoutScheduler.class).size());
            assertEquals(1, context.getBeansOfType(ReminderScheduler.class).size());
            var executor = context.getBean("flowTimeoutSchedulerExecutor",
                    ThreadPoolTaskScheduler.class);
            assertEquals(2, executor.getScheduledThreadPoolExecutor().getCorePoolSize());
            var processor = context.getBean(ScheduledAnnotationBeanPostProcessor.class);
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
                    assertTrue(context.getBeansOfType(ReminderScheduler.class).isEmpty());
                    assertFalse(context.containsBean("flowTimeoutSchedulerExecutor"));
                });
    }

    private FlowTaskDAO taskDAO() {
        FlowTaskDAO dao = mock(FlowTaskDAO.class);
        when(dao.selectList(any(Wrapper.class))).thenReturn(List.of());
        return dao;
    }

    private FlowTimeoutService timeoutService() {
        FlowTimeoutService service = mock(FlowTimeoutService.class);
        when(service.list(any(Wrapper.class))).thenReturn(List.of());
        return service;
    }
}
