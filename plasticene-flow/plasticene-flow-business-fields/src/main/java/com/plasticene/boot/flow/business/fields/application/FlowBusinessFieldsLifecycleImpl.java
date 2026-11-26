package com.plasticene.boot.flow.business.fields.application;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsCategoryService;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsFieldResolver;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsValueService;
import com.plasticene.boot.flow.core.api.spi.FlowBusinessFieldsLifecycle;
import com.plasticene.boot.flow.core.impl.application.FlowVariables;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import com.plasticene.boot.flow.core.model.enums.FlowFormMode;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 执行审批业务字段的发布和实例同步规则。
 * @author ZFJ
 * @since 2026-11-20
 */
@RequiredArgsConstructor
public class FlowBusinessFieldsLifecycleImpl implements FlowBusinessFieldsLifecycle {

    private final FlowBusinessFieldsFieldResolver resolver;
    private final FlowBusinessFieldsCategoryService categories;
    private final FlowBusinessFieldsValueService values;
    private final FlowVariables variables;

    @Override
    public FlowVariableSchema prepare(Publication publication) {
        boolean business = FlowFormMode.from(publication.formMode()) == FlowFormMode.BUSINESS;
        var schema = publication.schema();
        if (business) {
            var latest = variables.forBusiness(publication.orgId(), publication.processCode());
            if (!flags(schema).equals(flags(latest))) {
                throw new BizException("业务字段的列表资格已调整，请刷新变量后重新发布");
            }

        }
        var prepared = resolver.prepare(schema, business, publication.node());
        categories.validatePublication(publication, prepared);
        return prepared;
    }

    @Override
    public void sync(InstanceData instance) {
        values.sync(instance);
    }

    private Map<String, Boolean> flags(FlowVariableSchema schema) {
        return schema.getFields().stream().collect(Collectors.toMap(FlowVariableSchema.Field::getField,
                field -> Boolean.TRUE.equals(field.getAllowInList())));
    }
}
