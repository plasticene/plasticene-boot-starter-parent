package com.plasticene.boot.example.flow.provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 验证宿主只提供业务变量，其他模型使用框架默认空契约。
 * @author ZFJ
 * @since 2026-11-17
 */
class FlowAuthorizationConfigurationTest {
    @Test
    void shouldLeaveModelsWithoutVariablesToFrameworkDefault() {
        var provider = new FlowAuthorizationConfiguration().flowBusinessVariableProvider();
        assertNull(provider.getSchema(3L, "test-111602"));
        assertNull(provider.getSchema(4L, "another-business"));
    }

    @Test
    void shouldKeepLeaveVariableContract() {
        var schema = new FlowAuthorizationConfiguration().flowBusinessVariableProvider().getSchema(3L, "leave-approval");
        assertEquals(2, schema.getFields().size());
        assertEquals("days", schema.getFields().getFirst().getField());
    }

    @Test
    void shouldProvidePurchaseConditionVariables() {
        var schema = new FlowAuthorizationConfiguration().flowBusinessVariableProvider().getSchema(3L, "purchase-approval");
        assertEquals(3, schema.getFields().size());
        assertEquals("amount", schema.getFields().get(0).getField());
        assertEquals(1, schema.getFields().get(0).getType());
        assertEquals("category", schema.getFields().get(1).getField());
        assertEquals(0, schema.getFields().get(1).getType());
        assertEquals("urgent", schema.getFields().get(2).getField());
        assertEquals(1, schema.getFields().get(2).getType());
    }
}
