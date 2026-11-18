package com.plasticene.boot.flow.core.impl.application;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.api.param.*;
import com.plasticene.boot.flow.core.api.query.FlowInstanceQuery;
import com.plasticene.boot.flow.core.api.service.*;
import com.plasticene.boot.flow.core.api.spi.*;
import com.plasticene.boot.flow.core.impl.service.instance.FlowRuntimeService;
import jakarta.validation.Validation;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证新公开用例的参数、授权、查询范围及业务发起边界。
 * @author ZFJ
 * @since 2026-11-16
 */
class FlowApplicationBoundaryTest {
    private LoginUser actor() {
        var actor = new LoginUser(); actor.setId(8L); actor.setOrgId(9L); return actor;
    }
    @Test
    void publicContractsRequireExplicitActorAndDoNotExposeOrm() {
        for (var contract : List.of(FlowModelApi.class, FlowInstanceApi.class,
                FlowTaskApi.class, FlowQueryApi.class, FlowFormApi.class,
                FlowCategoryApi.class, FlowEventApi.class)) {
            assertEquals(0, contract.getInterfaces().length);
            for (var method : contract.getDeclaredMethods()) {
                var parameters = method.getParameterTypes();
                assertEquals(LoginUser.class, parameters[parameters.length - 1]);
                assertFalse(method.toGenericString().contains("core.impl"));
            }
        }
    }
    @Test
    void inputValidationRejectsMissingBusinessIdBeforeRuntime() {
        var application = new FlowInstanceApiImpl();
        var access = new FlowAccess();
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            ReflectionTestUtils.setField(access, "validator", factory.getValidator());
            ReflectionTestUtils.setField(application, "access", access);
            var runtime = mock(FlowRuntimeService.class);
            ReflectionTestUtils.setField(application, "runtimeService", runtime);
            var param = new FlowStartByIdParam(); param.setDefinitionId(31L);
            assertThrows(BizException.class, () -> application.startInstanceById(param, actor()));
            verifyNoInteractions(runtime);
        }
    }
    @Test
    void bothFormAndBusinessUseOneAuthorizedRuntimeBoundary() {
        var application = new FlowInstanceApiImpl();
        var access = mock(FlowAccess.class);
        var runtime = mock(FlowRuntimeService.class);
        ReflectionTestUtils.setField(application, "access", access);
        ReflectionTestUtils.setField(application, "runtimeService", runtime);
        var actor = actor();
        var form = new FlowStartParam(); form.setDefinitionId(31L); form.setVarMap(Map.of("amount", 5));
        when(runtime.startInstance(31L, form.getVarMap(), actor)).thenReturn(21L);
        assertEquals(21L, application.startInstance(form, actor));
        verify(access).check(eq(actor), eq(FlowAction.INSTANCE_START), any());
        var business = new FlowStartByIdParam(); business.setDefinitionId(32L);
        business.setBusinessId("request"); business.setVarMap(Map.of("amount", 5));
        when(runtime.startInstance(32L, "request", business.getVarMap(), actor)).thenReturn(22L);
        assertEquals(22L, application.startInstanceById(business, actor));
        verify(access).requireBusinessId("request");
    }
    @Test
    void defaultPolicyDeniesAndCrossOrganizationIsRejectedBeforeCallingHost() {
        var access = new FlowAccess();
        @SuppressWarnings("unchecked")
        ObjectProvider<FlowAuthorizationPolicy> policies = mock(ObjectProvider.class);
        ReflectionTestUtils.setField(access, "policies", policies);
        var actor = actor();
        assertThrows(BizException.class, () -> access.check(actor, FlowAction.MODEL_CREATE, access.organization(actor)));
        var resource = new FlowResource(10L, "MODEL", "1", 1L, null, null, null, null, null);
        reset(policies);
        assertThrows(BizException.class, () -> access.check(actor, FlowAction.MODEL_READ, resource));
        verifyNoInteractions(policies);
    }
    @Test
    void personalScopeCannotBeOverriddenByRequestAndOriginalQueryIsUnchanged() {
        var access = new FlowAccess();
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            ReflectionTestUtils.setField(access, "validator", factory.getValidator());
            var query = new FlowInstanceQuery(); query.setOrgId(999L); query.setUserId(100L); query.setStartUserId(100L);
            var scoped = access.scopeInstances(query, actor(), true);
            assertEquals(9L, scoped.getOrgId()); assertEquals(8L, scoped.getUserId()); assertEquals(8L, scoped.getStartUserId());
            assertEquals(999L, query.getOrgId());
        }
    }
    @Test
    void codeBasedStartExplicitlyAuthorizesTheResolvedDefinitionBeforeWriting() {
        var application = new FlowInstanceApiImpl();
        var access = mock(FlowAccess.class);
        var runtime = mock(FlowRuntimeService.class);
        ReflectionTestUtils.setField(application, "access", access);
        ReflectionTestUtils.setField(application, "runtimeService", runtime);
        var actor = actor();
        var param = new FlowStartByCodeParam();
        param.setProcessCode("purchase"); param.setBusinessId("request");
        var resource = new FlowResource(9L, "DEFINITION", "31", 11L, 31L, null, null, "purchase", "office");
        when(access.activeDefinition("purchase", actor)).thenReturn(31L);
        when(access.definition(31L)).thenReturn(resource);
        doThrow(new BizException("无权发起")).when(access).check(actor, FlowAction.INSTANCE_START, resource);
        assertThrows(BizException.class, () -> application.startInstanceByCode(param, actor));
        verify(access).check(actor, FlowAction.INSTANCE_START, resource);
        verifyNoInteractions(runtime);
    }
}
