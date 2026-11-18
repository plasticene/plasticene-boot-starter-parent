package com.plasticene.boot.flow.core.impl.application;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import com.plasticene.boot.flow.core.impl.form.FlowFormConfig;
import com.plasticene.boot.flow.core.api.spi.FlowBusinessVariableProvider;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowModel;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证变量类型、可信系统字段和表单内容字段的边界。
 * @author ZFJ
 * @since 2026-11-16
 */
class FlowVariablesTest {
    private final FlowVariables variables = new FlowVariables();
    @Test
    void businessModelsDefaultToEmptyContractWithoutProviderOrRegistration() {
        var model = new FlowModel();
        model.setFormMode(1);
        model.setOrgId(9L);
        model.setCode("new-business");
        var beans = new StaticListableBeanFactory();
        ReflectionTestUtils.setField(variables, "providers", beans.getBeanProvider(FlowBusinessVariableProvider.class));
        var empty = variables.forModel(model);
        assertTrue(empty.getFields().isEmpty());
        assertDoesNotThrow(() -> variables.validateData(empty, Map.of(), false, true));
        assertThrows(BizException.class,
                () -> variables.validateData(empty, Map.of("undeclared", 1), false, true));

        beans.addBean("variables", (FlowBusinessVariableProvider) (orgId, code) -> null);
        assertTrue(variables.forModel(model).getFields().isEmpty());
        beans.addBean("variables", (FlowBusinessVariableProvider) (orgId, code) -> {
            throw new IllegalStateException("provider failed");
        });
        assertThrows(IllegalStateException.class, () -> variables.forModel(model));
    }

    @Test
    void publishingBusinessModelRejectsChangedOrUnconfirmedHostSchema() {
        var model = new FlowModel();
        model.setFormMode(1);
        model.setOrgId(9L);
        model.setCode("approval");
        var beans = new StaticListableBeanFactory();
        beans.addBean("variables", (FlowBusinessVariableProvider) (orgId, code) -> schema(1));
        ReflectionTestUtils.setField(variables, "providers", beans.getBeanProvider(FlowBusinessVariableProvider.class));
        assertEquals("业务变量已调整，请刷新变量并适配工作流配置后重新发布",
                assertThrows(BizException.class, () -> variables.forModel(model)).getMessage());
        model.setVarInfo(schema(0));
        assertThrows(BizException.class, () -> variables.forModel(model));
        model.setVarInfo(schema(1));
        assertSame(model.getVarInfo(), variables.forModel(model));
        model.getVarInfo().getFields().getFirst().setRequired(false);
        assertThrows(BizException.class, () -> variables.forModel(model));
        model.setVarInfo(schema(1));
        model.getVarInfo().getFields().getFirst().setTitle("申请金额");
        assertThrows(BizException.class, () -> variables.forModel(model));
        beans.addBean("variables", (FlowBusinessVariableProvider) (orgId, code) -> null);
        assertThrows(BizException.class, () -> variables.forModel(model));
    }

    @Test
    void publishingUsesConfirmedDraftAndIgnoresFieldOrder() {
        var model = new FlowModel();
        model.setFormMode(1);
        var draft = schema(1);
        var other = new FlowVariableSchema.Field();
        other.setField("reason");
        other.setTitle("原因");
        other.setType(0);
        var amount = draft.getFields().getFirst();
        draft.setFields(List.of(amount, other));
        model.setVarInfo(draft);
        var latest = new FlowVariableSchema();
        latest.setFields(List.of(other, amount));
        var beans = new StaticListableBeanFactory();
        beans.addBean("variables", (FlowBusinessVariableProvider) (orgId, code) -> latest);
        ReflectionTestUtils.setField(variables, "providers", beans.getBeanProvider(FlowBusinessVariableProvider.class));
        assertSame(draft, variables.forModel(model));
        latest.setFields(List.of(amount));
        assertThrows(BizException.class, () -> variables.forModel(model));
    }

    private FlowVariableSchema schema(int type) {
        var field = new FlowVariableSchema.Field(); field.setField("amount"); field.setTitle("金额");
        field.setType(type); field.setRequired(true);
        var schema = new FlowVariableSchema(); schema.setFields(List.of(field)); return schema;
    }
    @Test
    void businessNumbersAreTypedAndUnknownOrSystemKeysAreRejected() {
        var schema = schema(1);
        assertDoesNotThrow(() -> variables.validateData(schema, Map.of("amount", new BigDecimal("12.34")), false, true));
        assertThrows(BizException.class, () -> variables.validateData(schema, Map.of("amount", "12.34"), false, true));
        assertThrows(BizException.class, () -> variables.validateData(schema, Map.of("amount", 1, "extra", 2), false, true));
        assertThrows(BizException.class, () -> variables.validateData(schema, Map.of("amount", 1, "PROCESS_START_USER_ID", 9), false, true));
        assertThrows(BizException.class, () -> variables.validateData(schema, Map.of(), false, true));
    }
    @Test
    void partialEditsDoNotRevalidateUnchangedHistoricalFields() {
        assertDoesNotThrow(() -> variables.validateData(schema(1), Map.of(), true, false));
        assertThrows(BizException.class, () -> variables.validateData(schema(1), Map.of("amount", "invalid"), true, false));
    }
    @Test
    void formMetadataKeepsLayoutIndependentFieldsAndDoesNotRejectContentPayloads() {
        var schema = FlowFormConfig.variableSchema(Map.of("rule", List.of(
                Map.of("field", "amount", "title", "金额", "type", "inputNumber", "$required", true),
                Map.of("field", "attachment", "title", "附件", "type", "upload"))));
        assertEquals(1, schema.getFields().get(0).getType());
        assertNull(schema.getFields().get(1).getType());
        assertDoesNotThrow(() -> variables.validateData(schema, Map.of("amount", 5,
                "attachment", List.of(Map.of("url", "/example")), "detail", List.of(Map.of("item", 1))), false, false));
    }
    @Test
    void duplicateFieldContractsAreRejected() {
        var schema = schema(1); schema.setFields(List.of(schema.getFields().getFirst(), schema.getFields().getFirst()));
        assertThrows(BizException.class, () -> variables.validateSchema(schema, true));
    }
    @Test
    void javaPrimitiveCollectionsUseTheSameTypedContractAsJsonArrays() {
        var schema = schema(3); schema.getFields().getFirst().setElementType(1);
        assertDoesNotThrow(() -> variables.validateData(schema, Map.of("amount", new int[]{1, 2}), false, true));
        assertThrows(BizException.class, () -> variables.validateData(schema, Map.of("amount", List.of("one")), false, true));
    }
    @Test
    void decimalStringModePreservesExistingFormPrecisionWithoutRelaxingBusinessContracts() {
        var schema = schema(1); schema.getFields().getFirst().setProps(Map.of("stringMode", true));
        var data = Map.<String, Object>of("amount", "123456789012345678.123456789");
        assertDoesNotThrow(() -> variables.validateData(schema, data, false, false));
        assertEquals("123456789012345678.123456789", data.get("amount"));
        assertThrows(BizException.class, () -> variables.validateData(schema, data, false, true));
        assertThrows(BizException.class, () -> variables.validateData(schema, Map.of("amount", "invalid"), false, false));
    }
}
