package com.plasticene.boot.flow.core.impl.application;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.spi.FlowAction;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowModel;
import com.plasticene.boot.flow.core.impl.service.model.FlowModelService;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 验证草稿切换表单类型后可只读加载业务变量，无需先保存类型。
 * @author ZFJ
 * @since 2026-11-17
 */
class FlowModelVariableQueryTest {
    @Test
    void shouldReadHostContractWithoutChangingSavedFormMode() {
        var access = mock(FlowAccess.class);
        var variables = mock(FlowVariables.class);
        var service = mock(FlowModelService.class);
        var api = new FlowModelApiImpl();
        ReflectionTestUtils.setField(api, "access", access);
        ReflectionTestUtils.setField(api, "variables", variables);
        ReflectionTestUtils.setField(api, "modelService", service);
        var actor = new LoginUser();
        actor.setId(8L);
        actor.setOrgId(9L);
        var model = new FlowModel();
        model.setOrgId(9L);
        model.setCode("business-code");
        model.setFormMode(0);
        when(access.ownedModel(11L, actor)).thenReturn(model);
        var schema = new FlowVariableSchema();
        when(variables.forBusiness(9L, "business-code")).thenReturn(schema);

        assertSame(schema, api.getVariableSchema(11L, actor));
        assertEquals(0, model.getFormMode());
        verify(access).check(eq(actor), eq(FlowAction.MODEL_READ), any());
        verify(variables).forBusiness(9L, "business-code");
        verify(variables, never()).forModel(any());
        verifyNoInteractions(service);
    }
}
