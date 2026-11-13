package com.plasticene.boot.flow.web.autoconfigure;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.plasticene.boot.flow.autoconfigure.FlowAutoConfiguration;
import com.plasticene.boot.flow.autoconfigure.FlowEventConfiguration;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证流程HTTP适配的显式启用与应用类型条件。
 * @author ZFJ
 * @since 2026-11-12
 */
class FlowHttpConfigurationTest {
    @Test
    void shouldOnlyRegisterControllersWhenHttpIsExplicitlyEnabled() {
        httpRunner().run(context -> {
            assertThat(context).hasNotFailed();
            assertHttpControllers(context, false);
        });
        httpRunner().withPropertyValues("ptc.flow.http.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertHttpControllers(context, false);
        });
        httpRunner().withPropertyValues("ptc.flow.http.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertHttpControllers(context, true);
        });
    }

    @Test
    void shouldNotRegisterHttpInANonWebHostEvenWhenMvcIsAvailable() {
        new ApplicationContextRunner()
                .withBean(DataSource.class, () -> mock(DataSource.class))
                .withConfiguration(AutoConfigurations.of(MybatisPlusAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class, TransactionAutoConfiguration.class,
                        FlowAutoConfiguration.class, FlowEventConfiguration.class, FlowHttpConfiguration.class))
                .withPropertyValues("ptc.flow.event.enabled=false", "ptc.flow.http.enabled=true",
                        "mybatis-plus.mapper-locations=classpath*:/mapper/*.xml")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(ClassUtils.isPresent(
                            "org.springframework.web.servlet.DispatcherServlet", context.getClassLoader())).isTrue();
                    assertHttpControllers(context, false);
                });
    }

    private WebApplicationContextRunner httpRunner() {
        return new WebApplicationContextRunner()
                .withBean(DataSource.class, () -> mock(DataSource.class))
                .withConfiguration(AutoConfigurations.of(MybatisPlusAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class, TransactionAutoConfiguration.class,
                        FlowAutoConfiguration.class, FlowEventConfiguration.class, FlowHttpConfiguration.class))
                .withPropertyValues("ptc.flow.event.enabled=false", "mybatis-plus.mapper-locations=classpath*:/mapper/*.xml");
    }

    private void assertHttpControllers(ApplicationContext context, boolean enabled) {
        for (String name : new String[]{"flowCategoryController", "flowDashboardController", "flowEventController",
                "flowFormController", "flowInstanceController", "flowModelController", "flowTaskController"}) {
            assertThat(context.containsBean(name)).as(name).isEqualTo(enabled);
        }
    }
}
