package com.plasticene.boot.flow.core.impl;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.FlowEngine;
import com.plasticene.boot.flow.core.api.param.FlowCategoryParam;
import com.plasticene.boot.flow.core.api.param.FlowFormParam;
import com.plasticene.boot.flow.core.api.param.FlowModelEnableParam;
import com.plasticene.boot.flow.core.api.param.FlowModelParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskFormParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskTransferParam;
import com.plasticene.boot.flow.core.api.query.FlowInstanceQuery;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceDetailVO;
import com.plasticene.boot.flow.core.impl.service.category.FlowCategoryService;
import com.plasticene.boot.flow.core.impl.service.form.FlowFormService;
import com.plasticene.boot.flow.core.impl.service.instance.FlowRuntimeService;
import com.plasticene.boot.flow.core.impl.service.model.FlowModelService;
import com.plasticene.boot.flow.core.impl.service.task.FlowTaskService;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 验证业务门面的参数校验、公开契约适配及个人查询范围。
 * @author ZFJ
 * @since 2026-11-13
 */
class FlowEngineBusinessApiTest {
    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private final FlowEngine engine = new FlowEngineImpl();
    private final LoginUser actor = new LoginUser();
    private final FlowCategoryService categories = mock(FlowCategoryService.class);
    private final FlowFormService forms = mock(FlowFormService.class);
    private final FlowModelService models = mock(FlowModelService.class);
    private final FlowRuntimeService runtime = mock(FlowRuntimeService.class);
    private final FlowTaskService tasks = mock(FlowTaskService.class);

    @BeforeEach
    void setup() {
        actor.setId(8L);
        actor.setOrgId(9L);
        ReflectionTestUtils.setField(engine, "flowCategoryService", categories);
        ReflectionTestUtils.setField(engine, "flowFormService", forms);
        ReflectionTestUtils.setField(engine, "flowModelService", models);
        ReflectionTestUtils.setField(engine, "flowRuntimeService", runtime);
        ReflectionTestUtils.setField(engine, "flowTaskService", tasks);
        ReflectionTestUtils.setField(engine, "validator", VALIDATORS.getValidator());
    }

    @AfterAll
    static void closeValidator() {
        VALIDATORS.close();
    }

    @Test
    void shouldValidateMaintenanceParametersWithoutAController() {
        assertThrows(BizException.class, () -> engine.createCategory(new FlowCategoryParam(), actor));
        assertThrows(BizException.class, () -> engine.updateCategory(new FlowCategoryParam(), actor));
        assertThrows(BizException.class, () -> engine.createForm(new FlowFormParam(), actor));
        assertThrows(BizException.class, () -> engine.updateForm(new FlowFormParam(), actor));
        assertThrows(BizException.class, () -> engine.createModel(new FlowModelParam(), actor));
        assertThrows(BizException.class, () -> engine.updateModel(new FlowModelParam(), actor));
        assertThrows(BizException.class, () -> engine.setModelStatus(new FlowModelEnableParam(), actor));
        assertThrows(BizException.class, () -> engine.createModel(null, actor));
        verifyNoInteractions(categories, forms, models);
    }

    @Test
    void shouldValidateDefaultConstraintsAlongsideCreateAndUpdateGroups() {
        var param = new FlowModelParam();
        param.setId(11L);
        param.setCode("leave_request");
        param.setName("名".repeat(33));
        param.setCategory("office");
        assertThrows(BizException.class, () -> engine.createModel(param, actor));
        assertThrows(BizException.class, () -> engine.updateModel(param, actor));
        verifyNoInteractions(models);
    }

    @Test
    void shouldPassSharedMaintenanceParametersAndResultsWithoutCopying() {
        var category = new FlowCategoryParam();
        category.setName("办公");
        category.setCode("office");
        when(categories.createCategory(any(), same(actor))).thenReturn(11L);
        assertEquals(11L, engine.createCategory(category, actor));
        var captured = ArgumentCaptor.forClass(FlowCategoryParam.class);
        verify(categories).createCategory(captured.capture(), same(actor));
        assertSame(category, captured.getValue());
        assertEquals("office", captured.getValue().getCode());
        assertEquals("办公", captured.getValue().getName());
        category.setId(11L);
        engine.updateCategory(category, actor);
        verify(categories).updateCategory(same(category), same(actor));

        var form = new FlowFormParam();
        form.setName("请假表单");
        form.setCategory("office");
        form.setFormConfig(Map.of("rule", List.of()));
        when(forms.create(any(), same(actor))).thenReturn(12L);
        assertEquals(12L, engine.createForm(form, actor));
        verify(forms).create(same(form), same(actor));
        form.setId(12L);
        engine.updateForm(form, actor);
        verify(forms).update(same(form), same(actor));

        var model = new FlowModelParam();
        model.setName("请假流程");
        model.setCode("leave_request");
        model.setCategory("office");
        when(models.create(any(), same(actor))).thenReturn(13L);
        assertEquals(13L, engine.createModel(model, actor));
        verify(models).create(same(model), same(actor));
        model.setId(13L);
        engine.updateModel(model, actor);
        verify(models).update(same(model), same(actor));

        var stored = new com.plasticene.boot.flow.core.impl.model.vo.FlowModelVO();
        stored.setId(13L);
        stored.setActiveDefinitionId(31L);
        stored.setActiveVersion(2);
        when(models.detail(13L, actor)).thenReturn(stored);
        assertSame(stored, engine.getModel(13L, actor));
        assertEquals(31L, engine.getModel(13L, actor).getActiveDefinitionId());
        engine.publishModel(13L, actor);
        verify(models).publish(13L, actor);
        var status = new FlowModelEnableParam();
        status.setId(13L);
        status.setStatus(-1);
        engine.setModelStatus(status, actor);
        verify(models).enable(same(status), same(actor));
    }

    @Test
    void shouldSerializeSharedHttpFieldsAndValidatePublicParameters() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var param = mapper.readValue("{\"id\":11,\"code\":\"office\",\"name\":\"办公\"}",
                FlowCategoryParam.class);
        assertEquals("office", param.getCode());
        var validator = VALIDATORS.getValidator();
        assertTrue(validator.validate(param, com.plasticene.boot.flow.core.api.validation.group.Insert.class).isEmpty());
        param.setCode("bad code");
        assertTrue(validator.validate(param, com.plasticene.boot.flow.core.api.validation.group.Insert.class)
                .stream().anyMatch(v -> v.getPropertyPath().toString().equals("code")));
        param.setCode(null);
        assertTrue(validator.validate(param, com.plasticene.boot.flow.core.api.validation.group.Update.class).isEmpty());
        param.setId(null);
        assertTrue(validator.validate(param, com.plasticene.boot.flow.core.api.validation.group.Update.class)
                .stream().anyMatch(v -> v.getPropertyPath().toString().equals("id")));

        var category = new com.plasticene.boot.flow.core.impl.model.vo.FlowCategoryVO();
        category.setId(11L);
        category.setName("办公");
        when(categories.listCategory(actor)).thenReturn(List.of(category));
        assertSame(category, engine.listCategories(actor).getFirst());
        assertEquals("办公", mapper.readTree(mapper.writeValueAsString(category)).get("name").asText());

        var form = new com.plasticene.boot.flow.core.impl.model.vo.FlowFormVO();
        form.setId(12L);
        form.setFormConfig(Map.of("rule", List.of()));
        when(forms.detail(12L, actor)).thenReturn(form);
        when(forms.listAll(actor)).thenReturn(List.of(form));
        assertSame(form, engine.getForm(12L, actor));
        assertSame(form, engine.listForms(actor).getFirst());
        assertTrue(mapper.readTree(mapper.writeValueAsString(form)).get("formConfig").has("rule"));
    }

    @Test
    void shouldDeleteExactlyOneResourceAndRejectMissingIds() {
        engine.deleteCategory(11L, actor);
        engine.deleteForm(12L, actor);
        engine.deleteModel(13L, actor);
        verify(categories).delete(List.of(11L), actor);
        verify(forms).delete(12L, actor);
        verify(models).delete(List.of(13L), actor);
        assertThrows(BizException.class, () -> engine.deleteCategory(null, actor));
        assertThrows(BizException.class, () -> engine.deleteForm(null, actor));
        assertThrows(BizException.class, () -> engine.deleteModel(null, actor));
        verifyNoMoreInteractions(categories, forms, models);
    }

    @Test
    void shouldRejectUntrustedActorsBeforeCallingAnyService() {
        assertThrows(BizException.class, () -> engine.listCategories(null));
        assertThrows(BizException.class, () -> engine.listForms(new LoginUser()));
        assertThrows(BizException.class, () -> engine.getModel(13L, null));
        assertThrows(BizException.class, () -> engine.deleteForm(12L, new LoginUser()));
        assertThrows(BizException.class, () -> engine.startInstance(31L, Map.of(), null));
        verifyNoInteractions(categories, forms, models, runtime);
    }

    @Test
    void shouldStartStandaloneApplicationsWithoutABusinessId() {
        when(runtime.startInstance(31L, Map.of("days", 2), actor)).thenReturn(21L);
        assertEquals(21L, engine.startInstance(31L, Map.of("days", 2), actor));
        verify(runtime).startInstance(31L, Map.of("days", 2), actor);
        assertThrows(BizException.class, () -> engine.startInstance(null, Map.of(), actor));
        verifyNoMoreInteractions(runtime);
    }

    @Test
    void shouldKeepMyApplicationsScopedWithoutMutatingTheCallersQuery() {
        var query = new FlowInstanceQuery();
        query.setOrgId(99L);
        query.setUserId(98L);
        query.setStartUserId(97L);
        query.setCategory("office");
        engine.pageMyInstances(query, actor);
        var captured = ArgumentCaptor.forClass(FlowInstanceQuery.class);
        verify(runtime).page(captured.capture(), same(actor));
        assertEquals(9L, captured.getValue().getOrgId());
        assertEquals(8L, captured.getValue().getUserId());
        assertEquals(8L, captured.getValue().getStartUserId());
        assertEquals("office", captured.getValue().getCategory());
        assertEquals(99L, query.getOrgId());
        assertEquals(98L, query.getUserId());
        assertEquals(97L, query.getStartUserId());
        query.setStatus(99);
        assertThrows(BizException.class, () -> engine.pageMyInstances(query, actor));
        verifyNoMoreInteractions(runtime);
    }

    @Test
    void shouldPreserveTheSelectedTaskContextAndAllowInstanceOnlyDetails() {
        var detail = new FlowInstanceDetailVO();
        when(runtime.getInstanceDetail(21L, 11L, actor)).thenReturn(detail);
        assertSame(detail, engine.getInstance(21L, 11L, actor));
        engine.getInstance(21L, actor);
        verify(runtime).getInstanceDetail(21L, 11L, actor);
        verify(runtime).getInstanceDetail(21L, null, actor);
    }

    @Test
    void shouldValidateTaskActionsAndSaveWithoutAdvancingTheTask() {
        assertThrows(BizException.class, () -> engine.approveTask(new FlowTaskParam(), actor));
        assertThrows(BizException.class, () -> engine.transferTask(new FlowTaskTransferParam(), actor));
        var form = new FlowTaskFormParam();
        form.setTaskId(11L);
        form.setFormData(Map.of("days", 2));
        engine.saveTaskForm(form, actor);
        verify(tasks).saveForm(form, actor);
        verifyNoMoreInteractions(tasks);
    }

    @Test
    void shouldKeepThePublicContractFreeOfImplementationTypesAndBatchDeletes() {
        var visited = new java.util.HashSet<java.lang.reflect.Type>();
        for (var method : FlowEngine.class.getDeclaredMethods()) {
            assertPublicContract(method.getGenericReturnType(), visited);
            for (var parameter : method.getGenericParameterTypes()) {
                assertPublicContract(parameter, visited);
            }
            if (method.getName().startsWith("delete")) {
                assertArrayEquals(new Class<?>[]{Long.class, LoginUser.class}, method.getParameterTypes());
            }
        }
    }

    private void assertPublicContract(java.lang.reflect.Type type, java.util.Set<java.lang.reflect.Type> visited) {
        if (!visited.add(type)) {
            return;
        }
        if (type instanceof java.lang.reflect.ParameterizedType parameterized) {
            assertPublicContract(parameterized.getRawType(), visited);
            for (var argument : parameterized.getActualTypeArguments()) {
                assertPublicContract(argument, visited);
            }
        } else if (type instanceof Class<?> contract) {
            assertFalse(contract.getName().startsWith("com.plasticene.boot.flow.core.impl."), contract.getName());
            if (contract.getName().startsWith("com.plasticene.boot.flow.")) {
                for (var field : contract.getDeclaredFields()) {
                    if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        assertPublicContract(field.getGenericType(), visited);
                    }
                }
                if (contract.getGenericSuperclass() != null) {
                    assertPublicContract(contract.getGenericSuperclass(), visited);
                }
            }
        }
    }

}
