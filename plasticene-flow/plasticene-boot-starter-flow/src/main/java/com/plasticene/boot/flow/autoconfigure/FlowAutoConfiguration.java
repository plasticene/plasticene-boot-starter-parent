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
import com.plasticene.boot.flow.core.impl.application.FlowAccess;
import com.plasticene.boot.flow.core.impl.application.FlowCategoryApiImpl;
import com.plasticene.boot.flow.core.impl.application.FlowEventApiImpl;
import com.plasticene.boot.flow.core.impl.application.FlowFormApiImpl;
import com.plasticene.boot.flow.core.impl.application.FlowInstanceApiImpl;
import com.plasticene.boot.flow.core.impl.application.FlowModelApiImpl;
import com.plasticene.boot.flow.core.impl.application.FlowQueryApiImpl;
import com.plasticene.boot.flow.core.impl.application.FlowTaskApiImpl;
import com.plasticene.boot.flow.core.impl.application.FlowVariables;
import com.plasticene.boot.flow.core.impl.service.category.FlowCategoryServiceImpl;
import com.plasticene.boot.flow.core.impl.service.dashboard.FlowDashboardServiceImpl;
import com.plasticene.boot.flow.core.impl.service.definition.FlowDefinitionServiceImpl;
import com.plasticene.boot.flow.core.impl.service.form.FlowFormServiceImpl;
import com.plasticene.boot.flow.core.impl.service.instance.FlowRuntimeServiceImpl;
import com.plasticene.boot.flow.core.impl.service.model.FlowModelServiceImpl;
import com.plasticene.boot.flow.core.impl.service.task.FlowTaskServiceImpl;
import com.plasticene.boot.flow.core.impl.service.timeout.FlowTimeoutServiceImpl;
import com.plasticene.boot.flow.core.impl.task.FlowTaskAddSignOperations;
import com.plasticene.boot.flow.core.impl.task.FlowTaskApproval;
import com.plasticene.boot.flow.core.impl.task.FlowTaskCreation;
import com.plasticene.boot.flow.core.impl.task.FlowTaskExecution;
import com.plasticene.boot.flow.core.impl.task.FlowTaskLifecycle;
import com.plasticene.boot.flow.core.impl.task.FlowTaskQueries;
import com.plasticene.boot.flow.core.impl.task.FlowTaskReturn;
import com.plasticene.boot.flow.core.impl.task.FlowTaskWithdraw;
import com.plasticene.boot.flow.core.impl.validation.node.FlowNodeValidator;

/**
 * @author ZFJ
 * @date 2025/9/3
 */
@AutoConfiguration(afterName = "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration")
@Import({
        FlowAccess.class,
        FlowCategoryApiImpl.class,
        FlowEventApiImpl.class,
        FlowFormApiImpl.class,
        FlowInstanceApiImpl.class,
        FlowModelApiImpl.class,
        FlowQueryApiImpl.class,
        FlowTaskApiImpl.class,
        FlowVariables.class,
        FlowCategoryServiceImpl.class,
        FlowDashboardServiceImpl.class,
        FlowDefinitionServiceImpl.class,
        FlowFormServiceImpl.class,
        FlowRuntimeServiceImpl.class,
        FlowModelServiceImpl.class,
        FlowTaskServiceImpl.class,
        FlowTimeoutServiceImpl.class,
        FlowTaskAddSignOperations.class,
        FlowTaskApproval.class,
        FlowTaskCreation.class,
        FlowTaskExecution.class,
        FlowTaskLifecycle.class,
        FlowTaskQueries.class,
        FlowTaskReturn.class,
        FlowTaskWithdraw.class,
        FlowNodeValidator.class
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
