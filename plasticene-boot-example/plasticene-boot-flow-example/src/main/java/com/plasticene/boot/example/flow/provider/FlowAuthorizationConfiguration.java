package com.plasticene.boot.example.flow.provider;

import com.plasticene.boot.flow.core.api.spi.*;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 演示宿主基于可信登录身份授权及提供请假审批变量契约。
 * @author ZFJ
 * @since 2026-11-16
 */
@Configuration(proxyBeanMethods = false)
public class FlowAuthorizationConfiguration {
    @Bean
    public FlowAuthorizationPolicy flowAuthorizationPolicy() {
        return (actor, action, resource) -> {
            if (!Objects.equals(actor.getOrgId(), resource.orgId())) { return false; }
            return switch (action) {
                case INSTANCE_START, INSTANCE_READ, INSTANCE_CANCEL,
                     TASK_APPROVE, TASK_REJECT, TASK_SAVE_FORM, TASK_ADD_SIGN,
                     TASK_TRANSFER, TASK_RETURN, TASK_WITHDRAW -> true;
                default -> Objects.equals(actor.getIsAdmin(), 1);
            };
        };
    }

    @Bean
    public FlowBusinessVariableProvider flowBusinessVariableProvider() {
        return (orgId, code) -> {
            if (!"leave-approval".equals(code)) { throw new IllegalArgumentException("未接入业务流程变量"); }
            var days = new FlowVariableSchema.Field(); days.setField("days"); days.setTitle("请假天数"); days.setType(1); days.setRequired(true);
            var reason = new FlowVariableSchema.Field(); reason.setField("reason"); reason.setTitle("请假原因"); reason.setType(0); reason.setRequired(true);
            var schema = new FlowVariableSchema(); schema.setFields(List.of(days, reason)); return schema;
        };
    }
}
