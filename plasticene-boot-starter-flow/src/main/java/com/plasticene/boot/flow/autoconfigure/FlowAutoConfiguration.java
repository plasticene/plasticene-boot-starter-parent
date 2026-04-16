package com.plasticene.boot.flow.autoconfigure;

import com.plasticene.boot.flow.core.executor.DefaultProcessExecutor;
import com.plasticene.boot.flow.core.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.provider.DefaultFlowTaskAssigneeProvider;
import com.plasticene.boot.flow.core.provider.FlowOrganizationProvider;
import com.plasticene.boot.flow.core.provider.FlowTaskAssigneeProvider;
import com.plasticene.boot.flow.core.validator.ApproveNodeValidator;
import com.plasticene.boot.flow.core.validator.ConditionNodeValidator;
import com.plasticene.boot.flow.core.validator.NodeValidator;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author ZFJ
 * @date 2025/9/3
 */
@Configuration
@ComponentScan(basePackages = {"com.plasticene.boot.flow"})
@MapperScan(basePackages = {"com.plasticene.boot.flow.core.dao"})
public class FlowAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(FlowTaskAssigneeProvider.class)
    public FlowTaskAssigneeProvider flowTaskAssigneeProvider () {
        return new DefaultFlowTaskAssigneeProvider();
    }

    @Bean
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
