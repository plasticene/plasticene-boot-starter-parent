package com.plasticene.boot.flow.autoconfigure;

import com.plasticene.boot.flow.core.FlowEngine;
import com.plasticene.boot.flow.web.FlowController;
import com.plasticene.boot.flow.web.FlowWebActorResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 审批流可选 HTTP 接口自动配置测试。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
class FlowWebAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FlowWebAutoConfiguration.class))
            .withUserConfiguration(WebConfiguration.class);

    @Test
    void doesNotExposeControllersByDefault() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(FlowController.class));
    }

    @Test
    void exposesControllersOnlyWhenExplicitlyEnabled() {
        contextRunner.withPropertyValues("ptc.flow.web.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(FlowController.class);
                    assertThat(context).hasSingleBean(FlowWebActorResolver.class);

                    MockMvc mockMvc = MockMvcBuilders
                            .webAppContextSetup(context.getSourceApplicationContext())
                            .build();
                    mockMvc.perform(get("/flow/tasks/mine")
                                    .header("X-Flow-Tenant-Id", "tenant-a")
                                    .header("X-Flow-Operator-Id", "user-a")
                                    .header("X-Flow-Roles", "manager, finance"))
                            .andExpect(status().isOk())
                            .andExpect(content().json("[]"));
                    mockMvc.perform(get("/flow/tasks/mine"))
                            .andExpect(status().isBadRequest())
                            .andExpect(jsonPath("$.code").value("FLOW_REQUEST_INVALID"));
                });
    }

    @Test
    void backsOffForCustomActorResolver() {
        contextRunner.withPropertyValues("ptc.flow.web.enabled=true")
                .withBean(FlowWebActorResolver.class,
                        () -> request -> new com.plasticene.boot.flow.core.model.FlowActor(
                                "custom-tenant", "custom-user", java.util.Set.of()))
                .run(context -> assertThat(context).hasSingleBean(FlowWebActorResolver.class));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class WebConfiguration {

        @Bean
        FlowEngine flowEngine() {
            return (FlowEngine) Proxy.newProxyInstance(FlowEngine.class.getClassLoader(),
                    new Class<?>[]{FlowEngine.class}, (proxy, method, arguments) -> {
                        if (method.getName().equals("findMyTasks")) {
                            return List.of();
                        }
                        return null;
                    });
        }
    }
}
