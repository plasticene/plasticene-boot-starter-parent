package com.plasticene.boot.flow.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.plasticene.boot.flow.core.dao.FlowEventOutboxDAO;
import com.plasticene.boot.flow.core.dao.FlowInstanceDAO;
import com.plasticene.boot.flow.core.event.FlowEventAdministration;
import com.plasticene.boot.flow.core.event.FlowEventConsumer;
import com.plasticene.boot.flow.core.event.FlowEventConsumers;
import com.plasticene.boot.flow.core.event.FlowEventDispatcher;
import com.plasticene.boot.flow.core.event.FlowEventManagementPolicy;
import com.plasticene.boot.flow.core.event.FlowEventStore;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 装配流程事件持久化与可靠交付。
 * @author ZFJ
 * @since 2026-11-11
 */
@AutoConfiguration(after = FlowAutoConfiguration.class)
@EnableConfigurationProperties(FlowEventProperties.class)
public class FlowEventConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public FlowEventStore flowEventStore(DataSource dataSource, PlatformTransactionManager transactionManager,
                                        FlowEventOutboxDAO eventDAO, FlowInstanceDAO instanceDAO,
                                        FlowEventProperties properties) {
        ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        return new FlowEventStore(dataSource, transactionManager, eventDAO, instanceDAO, mapper, properties, Clock.systemDefaultZone());
    }

    @Bean
    @ConditionalOnMissingBean
    public FlowEventConsumers flowEventConsumers(ObjectProvider<FlowEventConsumer> consumers) {
        return new FlowEventConsumers(consumers.orderedStream().toList());
    }

    // 未启动自动投递时，仍需在销毁 Bean 时释放已创建的调度和工作线程资源。
    @Bean(destroyMethod = "stop")
    @ConditionalOnMissingBean
    public FlowEventDispatcher flowEventDispatcher(FlowEventStore store, FlowEventConsumers consumers,
                                                   FlowEventProperties properties) {
        return new FlowEventDispatcher(store, consumers, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public FlowEventAdministration flowEventAdministration(
            FlowEventStore store, ObjectProvider<FlowEventManagementPolicy> policies) {
        return new FlowEventAdministration(store,
                policies.getIfAvailable(() -> actor -> false));
    }
}
