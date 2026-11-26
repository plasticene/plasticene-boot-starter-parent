package com.plasticene.boot.example.flow.provider;

import com.plasticene.boot.flow.core.api.spi.FlowAuthorizationPolicy;
import com.plasticene.boot.flow.core.api.spi.FlowBusinessVariableProvider;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 演示宿主基于可信登录身份授权及提供请假、采购审批变量契约。
 * @author ZFJ
 * @since 2026-11-16
 */
@Configuration(proxyBeanMethods = false)
public class FlowAuthorizationConfiguration {
    @Bean
    public FlowAuthorizationPolicy flowAuthorizationPolicy() {
        return (actor, action, resource) -> {
            if (!Objects.equals(actor.getOrgId(), resource.orgId())) {
                return false;
            }
            return switch (action) {
                case INSTANCE_START, INSTANCE_READ, INSTANCE_CANCEL,
                     TASK_APPROVE, TASK_REJECT, TASK_SAVE_FORM, TASK_ADD_SIGN,
                     TASK_TRANSFER, TASK_RETURN, TASK_WITHDRAW, CATEGORY_READ -> true;
                default -> Objects.equals(actor.getIsAdmin(), 1);
            };
        };
    }

    @Bean
    public FlowBusinessVariableProvider flowBusinessVariableProvider() {
        return (orgId, code) -> {
            if ("purchase-approval".equals(code)) {
                var schema = new FlowVariableSchema();
                schema.setFields(List.of(
                        purchaseField("amount", "采购金额", 1, null),
                        purchaseField("category", "采购类别", 0, List.of(
                                Map.of("label", "办公用品", "value", "office"),
                                Map.of("label", "电子设备", "value", "electronics"),
                                Map.of("label", "其他", "value", "other"))),
                        purchaseField("urgent", "是否紧急", 1, List.of(
                                Map.of("label", "否", "value", 0),
                                Map.of("label", "是", "value", 1)))));
                return schema;
            }
            if (!"leave-approval".equals(code)) {
                return null;
            }
            var days = new FlowVariableSchema.Field();
            days.setField("days");
            days.setTitle("请假天数");
            days.setType(1);
            days.setRequired(true);
            var reason = new FlowVariableSchema.Field();
            reason.setField("reason");
            reason.setTitle("请假原因");
            reason.setType(0);
            reason.setRequired(true);
            var schema = new FlowVariableSchema();
            schema.setFields(List.of(days, reason));
            return schema;
        };
    }

    /** 构造采购流程变量，紧急程度沿用 FLOW 数字条件类型，以 0/1 表达。 */
    private FlowVariableSchema.Field purchaseField(String name, String title, int type,
                                                   List<Map<String, Object>> options) {
        var field = new FlowVariableSchema.Field();
        field.setField(name);
        field.setTitle(title);
        field.setType(type);
        field.setRequired(!"urgent".equals(name));
        field.setOptions(options);
        field.setAllowInList(!"urgent".equals(name));
        return field;
    }
}
