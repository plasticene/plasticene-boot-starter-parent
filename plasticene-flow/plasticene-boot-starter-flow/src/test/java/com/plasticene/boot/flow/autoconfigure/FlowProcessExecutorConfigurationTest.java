package com.plasticene.boot.flow.autoconfigure;

import com.plasticene.boot.flow.core.impl.executor.DefaultProcessExecutor;
import com.plasticene.boot.flow.core.impl.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.impl.service.instance.FlowRuntimeService;
import com.plasticene.boot.flow.core.impl.service.task.FlowTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.NestedExceptionUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * 验证默认执行器始终装配，不因宿主另一个同类型 Bean 退让。
 * @author ZFJ
 * @since 2026-11-10
 */
class FlowProcessExecutorConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FlowAutoConfiguration.class))
            .withBean("testFlowTaskService", FlowTaskService.class, () -> mock(FlowTaskService.class))
            .withBean("testFlowRuntimeService", FlowRuntimeService.class, () -> mock(FlowRuntimeService.class))
            .withInitializer(context -> context.addBeanFactoryPostProcessor(factory -> {
                // 使用真实自动配置的执行器 Bean 定义，隔离组件扫描带入的数据库、控制器和定时任务。
                BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;
                Set<String> retained = Set.of(FlowAutoConfiguration.class.getName(), "processExecutor", "customProcessExecutor",
                        "testFlowTaskService", "testFlowRuntimeService");
                for (String name : registry.getBeanDefinitionNames()) {
                    BeanDefinition definition = registry.getBeanDefinition(name);
                    if (definition.getRole() == BeanDefinition.ROLE_APPLICATION
                            && !retained.contains(name)
                            && !FlowAutoConfiguration.class.getName().equals(definition.getBeanClassName())) {
                        registry.removeBeanDefinition(name);
                    }
                }
            }));

    @Test
    void shouldRegisterDefaultExecutorWhenNoCustomExecutorExists() {
        runner.run(context -> {
            assertNull(context.getStartupFailure(), () -> NestedExceptionUtils
                    .getMostSpecificCause(context.getStartupFailure()).toString());
            assertEquals(1, context.getBeansOfType(ProcessExecutor.class).size());
            assertInstanceOf(DefaultProcessExecutor.class, context.getBean(ProcessExecutor.class));
        });
    }

    @Test
    void shouldRetainInternalExecutorWhenAnotherTypeBeanIsPresent() {
        ProcessExecutor custom = mock(ProcessExecutor.class);
        runner.withBean("customProcessExecutor", ProcessExecutor.class, () -> custom).run(context -> {
            assertNull(context.getStartupFailure(), () -> NestedExceptionUtils
                    .getMostSpecificCause(context.getStartupFailure()).toString());
            assertEquals(2, context.getBeansOfType(ProcessExecutor.class).size());
            assertInstanceOf(DefaultProcessExecutor.class, context.getBean("processExecutor"));
        });
    }

    @Test
    void shouldRejectAUserBeanWithTheInternalExecutorName() {
        ProcessExecutor custom = mock(ProcessExecutor.class);
        runner.withAllowBeanDefinitionOverriding(false)
                .withBean("processExecutor", ProcessExecutor.class, () -> custom)
                .run(context -> assertNotNull(context.getStartupFailure()));
    }
}
