package com.plasticene.boot.flow.autoconfigure;

import com.plasticene.boot.flow.core.FlowEngine;
import com.plasticene.boot.flow.web.FlowController;
import com.plasticene.boot.flow.web.FlowWebActorResolver;
import com.plasticene.boot.flow.web.FlowWebExceptionHandler;
import com.plasticene.boot.flow.web.HeaderFlowWebActorResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * 审批流可选 HTTP 接口自动配置。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
@AutoConfiguration(after = FlowAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnBean(FlowEngine.class)
@ConditionalOnProperty(prefix = "ptc.flow.web", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(FlowProperties.class)
public class FlowWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FlowWebActorResolver flowWebActorResolver(FlowProperties properties) {
        return new HeaderFlowWebActorResolver(properties.getWeb());
    }

    @Bean
    @ConditionalOnMissingBean
    public FlowController flowController(FlowEngine flowEngine, FlowWebActorResolver actorResolver) {
        return new FlowController(flowEngine, actorResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public FlowWebExceptionHandler flowWebExceptionHandler() {
        return new FlowWebExceptionHandler();
    }
}
