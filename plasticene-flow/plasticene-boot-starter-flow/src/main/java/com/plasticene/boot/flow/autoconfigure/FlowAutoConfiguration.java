package com.plasticene.boot.flow.autoconfigure;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasticene.boot.flow.core.FlowEngine;
import com.plasticene.boot.flow.core.spi.FlowAssigneeProvider;
import com.plasticene.boot.flow.core.spi.FlowEventHandler;
import com.plasticene.boot.flow.core.spi.FlowRepository;
import com.plasticene.boot.flow.engine.DefaultFlowEngine;
import com.plasticene.boot.flow.engine.FlowModelValidator;
import com.plasticene.boot.flow.engine.FlowTransitionService;
import com.plasticene.boot.flow.mybatis.MybatisFlowRepository;
import com.plasticene.boot.flow.mybatis.mapper.FlowPersistenceMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.ApplicationEventPublisher;

import javax.sql.DataSource;
import java.time.Clock;

/**
 * 审批流 Starter 自动配置。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
@AutoConfiguration
@AutoConfigureAfter({DataSourceAutoConfiguration.class, MybatisPlusAutoConfiguration.class})
@ConditionalOnClass({FlowEngine.class, FlowPersistenceMapper.class})
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(prefix = "ptc.flow", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(FlowProperties.class)
@MapperScan("com.plasticene.boot.flow.mybatis.mapper")
public class FlowAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper flowObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    @ConditionalOnMissingBean
    public Clock flowClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    @ConditionalOnMissingBean
    public FlowAssigneeProvider flowAssigneeProvider() {
        return (actor, definition, node) -> node.configuredAssignees();
    }

    @Bean
    @ConditionalOnMissingBean
    public FlowRepository flowRepository(FlowPersistenceMapper mapper, ObjectMapper objectMapper) {
        return new MybatisFlowRepository(mapper, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public FlowModelValidator flowModelValidator() {
        return new FlowModelValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public FlowTransitionService flowTransitionService(FlowRepository repository,
                                                       FlowAssigneeProvider assigneeProvider, Clock clock) {
        return new FlowTransitionService(repository, assigneeProvider, clock);
    }

    @Bean
    @ConditionalOnMissingBean(FlowEngine.class)
    public FlowEngine flowEngine(FlowRepository repository, FlowModelValidator modelValidator,
                                 FlowTransitionService transitionService, Clock clock) {
        return new DefaultFlowEngine(repository, modelValidator, transitionService, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public FlowEventHandler flowEventHandler(ApplicationEventPublisher publisher) {
        return publisher::publishEvent;
    }

    @Bean
    @DependsOn("flowSchemaManager")
    @ConditionalOnProperty(prefix = "ptc.flow.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public FlowOutboxDispatcher flowOutboxDispatcher(FlowRepository repository,
                                                     java.util.List<FlowEventHandler> handlers,
                                                     FlowProperties properties, Clock clock) {
        return new FlowOutboxDispatcher(repository, handlers, properties.getOutbox(), clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public FlowSchemaManager flowSchemaManager(DataSource dataSource, FlowProperties properties) {
        return new FlowSchemaManager(dataSource, properties.getDatabase().getSchemaAction());
    }
}
