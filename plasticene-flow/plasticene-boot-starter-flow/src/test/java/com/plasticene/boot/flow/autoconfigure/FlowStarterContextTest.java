package com.plasticene.boot.flow.autoconfigure;

import com.plasticene.boot.flow.core.config.FlowEventProperties;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.plasticene.boot.flow.core.api.FlowEngine;
import com.plasticene.boot.flow.core.impl.event.FlowEventStore;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证不含 Web 体系的宿主可以装配核心流程能力。
 * @author ZFJ
 * @since 2026-11-11
 */
class FlowStarterContextTest {
    @Test
    void shouldKeepMvcAndPlasticeneWebOffTheStarterClasspath() {
        ClassLoader loader = getClass().getClassLoader();
        assertThat(ClassUtils.isPresent("org.springframework.web.servlet.DispatcherServlet", loader)).isFalse();
        assertThat(ClassUtils.isPresent("com.plasticene.boot.web.autoconfigure.PlasticeneWebAutoConfiguration", loader)).isFalse();
    }

    @Test
    void shouldLoadCoreWithoutMvcOrPlasticeneWeb() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("org.springframework.web", "com.plasticene.boot.web",
                        "org.springframework.cloud.openfeign", "jakarta.servlet"))
                .withBean(DataSource.class, () -> mock(DataSource.class))
                .withConfiguration(AutoConfigurations.of(MybatisPlusAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class, TransactionAutoConfiguration.class,
                        FlowAutoConfiguration.class, FlowEventConfiguration.class))
                .withPropertyValues("ptc.flow.event.enabled=false", "ptc.flow.http.enabled=true",
                        "mybatis-plus.mapper-locations=classpath*:/mapper/*.xml")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(FlowEngine.class).hasSingleBean(FlowEventStore.class);
                    assertThat(context).doesNotHaveBean("flowInstanceController");
                    assertThat(context).doesNotHaveBean("flowTaskController");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ptc.flow.event.workers=0", "ptc.flow.event.max-retries=-1",
            "ptc.flow.event.lease=5s", "ptc.flow.event.poll-interval=0s"
    })
    void shouldRejectInvalidEventPropertiesWithoutDependingOnDispatcherCreation(String property) {
        new ApplicationContextRunner().withUserConfiguration(EventPropertiesConfiguration.class)
                .withPropertyValues(property)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                });
    }

    @Test
    void shouldBindValidEventPropertiesWithoutDispatcher() {
        new ApplicationContextRunner().withUserConfiguration(EventPropertiesConfiguration.class)
                .withPropertyValues("ptc.flow.event.workers=2")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(FlowEventProperties.class).getWorkers()).isEqualTo(2);
                });
    }

    /**
     * 单独装配事件配置对象，验证绑定校验不依赖其他组件。
     * @author ZFJ
     * @since 2026-11-11
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(FlowEventProperties.class)
    static class EventPropertiesConfiguration {
    }
}
