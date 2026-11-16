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
            var lifecycle = context.getBean("flowTimeoutLifecycle", org.springframework.context.SmartLifecycle.class);
            assertTrue(lifecycle.isRunning());
            lifecycle.stop();
            assertFalse(lifecycle.isRunning());
            lifecycle.start();
            assertTrue(lifecycle.isRunning());
        });
    }

    @Test
    void shouldRegisterReminderSchedulerWhenNotificationIsProvided() {
        FlowTimeoutNotification channel = message -> {};
        runner.withBean(FlowTimeoutNotification.class, () -> channel).run(context -> {
            assertNull(context.getStartupFailure());
            assertSame(channel, context.getBean(FlowTimeoutNotification.class));
            assertEquals(1, context.getBeansOfType(FlowTimeoutScheduler.class).size());
            assertTrue(context.getBeansOfType(ReminderScheduler.class).isEmpty());
            var executor = context.getBean("flowTimeoutSchedulerExecutor",
                    ThreadPoolTaskScheduler.class);
            assertEquals(2, executor.getScheduledThreadPoolExecutor().getCorePoolSize());
            assertTrue(context.getBean("flowTimeoutLifecycle", org.springframework.context.SmartLifecycle.class).isRunning());
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

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldRegisterTwoTasksOnlyWithAChannelAndCancelOnlyOwnFutures() {
        var executor = mock(ThreadPoolTaskScheduler.class);
        java.util.concurrent.ScheduledFuture first = mock(java.util.concurrent.ScheduledFuture.class);
        java.util.concurrent.ScheduledFuture second = mock(java.util.concurrent.ScheduledFuture.class);
        org.mockito.Mockito.when(executor.scheduleWithFixedDelay(any(Runnable.class), any(java.time.Duration.class)))
                .thenReturn(first, second);
        runner.withBean("flowTimeoutSchedulerExecutor", ThreadPoolTaskScheduler.class, () -> executor)
                .withBean(FlowTimeoutNotification.class, () -> message -> { })
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    org.mockito.Mockito.verify(executor, org.mockito.Mockito.times(2))
                            .scheduleWithFixedDelay(any(Runnable.class), org.mockito.ArgumentMatchers.eq(java.time.Duration.ofSeconds(30)));
                    context.getBean("flowTimeoutLifecycle", org.springframework.context.SmartLifecycle.class).stop();
                    org.mockito.Mockito.verify(first).cancel(false);
                    org.mockito.Mockito.verify(second).cancel(false);
                    org.mockito.Mockito.verify(executor, org.mockito.Mockito.never()).shutdown();
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
