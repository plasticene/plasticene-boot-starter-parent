package com.plasticene.boot.flow.autoconfigure;

import com.plasticene.boot.flow.core.impl.executor.DefaultProcessExecutor;
import com.plasticene.boot.flow.core.impl.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.impl.task.DefaultFlowTaskAssigneeProvider;
import com.plasticene.boot.flow.core.api.spi.FlowOrganizationProvider;
import com.plasticene.boot.flow.core.api.spi.FlowTaskAssigneeProvider;
import com.plasticene.boot.flow.core.impl.validation.node.ApproveNodeValidator;
import com.plasticene.boot.flow.core.impl.validation.node.ConditionNodeValidator;
import com.plasticene.boot.flow.core.impl.validation.node.NodeValidator;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * @author ZFJ
 * @date 2025/9/3
 */
@AutoConfiguration(afterName = "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration")
@ComponentScan(basePackages = "com.plasticene.boot.flow.core.impl")
@MapperScan(basePackages = {"com.plasticene.boot.flow.core.impl.persistence.dao"})
public class FlowAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(FlowTaskAssigneeProvider.class)
    public FlowTaskAssigneeProvider flowTaskAssigneeProvider () {
        return new DefaultFlowTaskAssigneeProvider();
    }

    @Bean
    @ConditionalOnMissingBean(ProcessExecutor.class)
    public ProcessExecutor processExecutor () {
        return new DefaultProcessExecutor();
    }

    @Bean
    @ConditionalOnMissingBean(FlowOrganizationProvider.class)
    public FlowOrganizationProvider flowOrganizationProvider () {
        return new FlowOrganizationProvider() {};
    }

    @Bean
    @ConditionalOnMissingBean(name = "approveNodeValidator")
    public NodeValidator approveNodeValidator () {
        return new ApproveNodeValidator();
    }

    @Bean
    @ConditionalOnMissingBean(name = "conditionNodeValidator")
    public NodeValidator conditionNodeValidator () {
        return new ConditionNodeValidator();

    }


}
