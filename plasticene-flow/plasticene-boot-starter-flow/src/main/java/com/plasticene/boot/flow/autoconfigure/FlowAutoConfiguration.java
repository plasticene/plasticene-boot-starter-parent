package com.plasticene.boot.flow.autoconfigure;

import com.plasticene.boot.flow.core.api.spi.FlowOrganizationProvider;
import com.plasticene.boot.flow.core.api.spi.FlowTaskAssigneeProvider;
import com.plasticene.boot.flow.core.impl.executor.DefaultProcessExecutor;
import com.plasticene.boot.flow.core.impl.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.impl.task.DefaultFlowTaskAssigneeProvider;
import com.plasticene.boot.flow.core.impl.validation.node.ApproveNodeValidator;
import com.plasticene.boot.flow.core.impl.validation.node.ConditionNodeValidator;
import com.plasticene.boot.flow.core.impl.validation.node.NodeValidator;
import jakarta.validation.Validator;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * @author ZFJ
 * @date 2025/9/3
 */
@AutoConfiguration(afterName = "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration")
@Import({
        com.plasticene.boot.flow.core.impl.application.FlowAccess.class,
        com.plasticene.boot.flow.core.impl.application.FlowCategoryApplication.class,
        com.plasticene.boot.flow.core.impl.application.FlowEventApplication.class,
        com.plasticene.boot.flow.core.impl.application.FlowFormApplication.class,
        com.plasticene.boot.flow.core.impl.application.FlowInstanceApplication.class,
        com.plasticene.boot.flow.core.impl.application.FlowModelApplication.class,
        com.plasticene.boot.flow.core.impl.application.FlowQueryApplication.class,
        com.plasticene.boot.flow.core.impl.application.FlowTaskApplication.class,
        com.plasticene.boot.flow.core.impl.application.FlowVariables.class,
        com.plasticene.boot.flow.core.impl.service.category.FlowCategoryServiceImpl.class,
        com.plasticene.boot.flow.core.impl.service.dashboard.FlowDashboardServiceImpl.class,
        com.plasticene.boot.flow.core.impl.service.definition.FlowDefinitionServiceImpl.class,
        com.plasticene.boot.flow.core.impl.service.form.FlowFormServiceImpl.class,
        com.plasticene.boot.flow.core.impl.service.instance.FlowRuntimeServiceImpl.class,
        com.plasticene.boot.flow.core.impl.service.model.FlowModelServiceImpl.class,
        com.plasticene.boot.flow.core.impl.service.task.FlowTaskServiceImpl.class,
        com.plasticene.boot.flow.core.impl.service.timeout.FlowTimeoutServiceImpl.class,
        com.plasticene.boot.flow.core.impl.task.FlowTaskAddSignOperations.class,
        com.plasticene.boot.flow.core.impl.task.FlowTaskApproval.class,
        com.plasticene.boot.flow.core.impl.task.FlowTaskCreation.class,
        com.plasticene.boot.flow.core.impl.task.FlowTaskExecution.class,
        com.plasticene.boot.flow.core.impl.task.FlowTaskLifecycle.class,
        com.plasticene.boot.flow.core.impl.task.FlowTaskQueries.class,
        com.plasticene.boot.flow.core.impl.task.FlowTaskReturn.class,
        com.plasticene.boot.flow.core.impl.task.FlowTaskWithdraw.class,
        com.plasticene.boot.flow.core.impl.validation.node.FlowNodeValidator.class
})
@MapperScan(basePackages = {"com.plasticene.boot.flow.core.impl.persistence.dao"})
public class FlowAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Validator.class)
    public LocalValidatorFactoryBean flowValidator() {
        return new LocalValidatorFactoryBean();
    }

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
