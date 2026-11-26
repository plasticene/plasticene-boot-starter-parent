package com.plasticene.boot.flow.business.fields;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.business.fields.persistence.dao.FlowInstanceFieldValueDAO;
import com.plasticene.boot.flow.business.fields.persistence.entity.FlowInstanceFieldValue;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsCategoryService;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsFieldResolver;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsQuerySupport;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsValueService;
import com.plasticene.boot.flow.core.api.param.FlowBusinessFieldFilter;
import com.plasticene.boot.flow.core.api.spi.FlowBusinessFieldsLifecycle.InstanceData;
import com.plasticene.boot.flow.core.api.vo.FlowBusinessFieldsVO.Field;
import com.plasticene.boot.flow.core.model.FlowFieldPermission;
import com.plasticene.boot.flow.core.model.FlowNode;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证发布资格、规范化冲突与搜索精度的边界。
 * @author ZFJ
 * @since 2026-11-20
 */
class FlowBusinessFieldsRulesTest {

    private final FlowBusinessFieldsFieldResolver resolver = new FlowBusinessFieldsFieldResolver();

    @Test
    void excludesFieldsHiddenInAnyBranchAndDoesNotMutateDraft() {
        var amount = field("Amount", 1);
        var quote = field("internalQuote", 1);
        var approval = new FlowNode();
        approval.setType(1);
        var hidden = new FlowFieldPermission();
        hidden.setField("internalQuote");
        hidden.setPermission(FlowFieldPermission.Permission.HIDDEN);
        approval.setFieldsPermission(List.of(hidden));
        var branch = new FlowNode();
        branch.setChildNode(approval);
        var start = new FlowNode();
        start.setType(0);
        start.setConditionNodes(List.of(branch));

        var result = resolver.prepare(schema(amount, quote), false, start);
        assertEquals("Amount", resolver.descriptors(result).get("amount").sourceField());
        assertFalse(result.getFields().get(1).getAllowInList());
        assertNull(amount.getAllowInList());
        assertEquals(Map.of(), resolver.published(result));
    }

    @Test
    void rejectsOptionsThatNormalizeToTheSameString() {
        var field = field("Urgent", 1);
        field.setAllowInList(true);
        field.setOptions(List.of(Map.of("label", "是", "value", 1), Map.of("label", "重复", "value", "1")));
        assertThrows(BizException.class, () -> resolver.prepare(schema(field), true, null));
        assertEquals(1, field.getOptions().getFirst().get("value"));
    }

    @Test
    void compilesBoundPreciseAmountAndLiteralPrefix() {
        var categories = mock(FlowBusinessFieldsCategoryService.class);
        when(categories.searchable(7L, "purchase")).thenReturn(List.of(
                new Field("amount", "采购金额", "NUMBER", 1, true, true, 1, List.of()),
                new Field("supplier", "供应商", "TEXT", 0, true, true, 2, List.of())));
        var query = new FlowBusinessFieldsQuerySupport(categories, null, resolver, null);
        var conditions = query.compile(7L, "purchase", List.of(
                new FlowBusinessFieldFilter("aMount", "GT", "99999999999999.9999", null),
                new FlowBusinessFieldFilter("SUPPLIER", "STARTS_WITH", "A_%!", null)));
        assertEquals(new BigDecimal("99999999999999.9999"), conditions.getFirst().value());
        assertEquals("a!_!%!!%", conditions.getLast().value());
        assertThrows(BizException.class, () -> query.compile(7L, null, List.of(
                new FlowBusinessFieldFilter("amount", "GT", "0", null))));
        assertThrows(IllegalArgumentException.class, () -> FlowBusinessFieldsValueService.number("0.00001"));
    }

    @Test
    void validatesAllValuesBeforeWritingAndKeepsZero() {
        var amount = field("Amount", 1);
        amount.setAllowInList(true);
        var choice = field("Urgent", 1);
        choice.setAllowInList(true);
        choice.setOptions(List.of(Map.of("value", 1, "label", "是")));
        var schema = schema(amount, choice);
        schema.setListVersion(1);
        var mapper = mock(FlowInstanceFieldValueDAO.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        var values = new FlowBusinessFieldsValueService(mapper, resolver);
        assertThrows(BizException.class, () -> values.sync(
                new InstanceData(3L, 5L, 7L, schema, Map.of("Amount", 0, "Urgent", 2))));
        verify(mapper, never()).insert(anyList());
        values.sync(new InstanceData(3L, 5L, 7L, schema, Map.of("Amount", 0, "Urgent", 1)));
        verify(mapper).insert(argThat((List<FlowInstanceFieldValue> rows) ->
                rows.size() == 2 && BigDecimal.ZERO.equals(rows.getFirst().getNumberValue())
                        && "1".equals(rows.getLast().getMatchValue())));
    }

    private FlowVariableSchema.Field field(String id, int type) {
        var field = new FlowVariableSchema.Field();
        field.setField(id);
        field.setTitle(id);
        field.setType(type);
        return field;
    }

    private FlowVariableSchema schema(FlowVariableSchema.Field... fields) {
        var schema = new FlowVariableSchema();
        schema.setFields(List.of(fields));
        return schema;
    }
}
