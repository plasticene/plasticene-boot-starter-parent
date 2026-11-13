package com.plasticene.boot.flow.core.impl;

import com.plasticene.boot.flow.core.api.FlowEngine;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowModelDAO;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowModel;
import com.plasticene.boot.flow.core.impl.service.instance.FlowRuntimeService;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 验证按定义及编码发起、可选变量和必填业务申请 ID 的门面契约。
 * @author ZFJ
 * @since 2026-11-12
 */
class FlowEngineStartTest {
    private final FlowRuntimeService runtime = mock(FlowRuntimeService.class);
    private final FlowModelDAO models = mock(FlowModelDAO.class);
    private final FlowEngine engine = new FlowEngineImpl();
    private final LoginUser actor = new LoginUser();

    @BeforeEach
    void setup() {
        actor.setId(8L);
        actor.setOrgId(9L);
        ReflectionTestUtils.setField(engine, "flowRuntimeService", runtime);
        ReflectionTestUtils.setField(engine, "flowModelDAO", models);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "engine-start"), FlowModel.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldStartByDefinitionWithOrWithoutVariables(boolean withVariables) {
        Map<String, Object> variables = withVariables ? Map.of("days", 2) : null;
        when(runtime.startInstance(31L, "request", variables, actor)).thenReturn(21L);
        Long id = withVariables ? engine.startInstanceById(31L, "request", variables, actor)
                : engine.startInstanceById(31L, "request", actor);
        assertEquals(21L, id);
        verify(runtime).startInstance(31L, "request", variables, actor);
        verifyNoInteractions(models);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldResolveCodeInsideTheActorsOrganizationAndForwardVariables(boolean withVariables) {
        var model = new FlowModel();
        model.setActiveDefinitionId(31L);
        when(models.selectOne(any(Wrapper.class))).thenAnswer(invocation -> {
            Wrapper<FlowModel> query = invocation.getArgument(0);
            assertTrue(query.getSqlSegment().contains("org_id"));
            assertTrue(query.getSqlSegment().contains("code ="));
            assertFalse(query.getSqlSegment().contains("BINARY"));
            return model;
        });
        Map<String, Object> variables = withVariables ? Map.of("days", 2) : null;
        when(runtime.startInstance(31L, "request", variables, actor)).thenReturn(21L);
        Long id = withVariables ? engine.startInstanceByCode("leave", "request", variables, actor)
                : engine.startInstanceByCode("leave", "request", actor);
        assertEquals(21L, id);
        verify(runtime).startInstance(31L, "request", variables, actor);
    }

    @Test
    void shouldRejectMissingBlankAndOversizedBusinessIdsBeforeAnyDatabaseCall() {
        for (String businessId : new String[]{null, "", " ", "x".repeat(129)}) {
            assertThrows(BizException.class, () -> engine.startInstanceById(31L, businessId, actor));
            assertThrows(BizException.class, () -> engine.startInstanceByCode("leave", businessId, actor));
        }
        verifyNoInteractions(runtime, models);
    }

    @Test
    void shouldRequireADefinitionIdOrNonblankCodeInTheRespectiveEntry() {
        assertThrows(BizException.class, () -> engine.startInstanceById(null, "request", actor));
        assertThrows(BizException.class, () -> engine.startInstanceByCode(null, "request", actor));
        assertThrows(BizException.class, () -> engine.startInstanceByCode(" ", "request", actor));
        verifyNoInteractions(runtime, models);
    }

    @Test
    void shouldRejectMissingOrUnpublishedModelsBeforeStarting() {
        when(models.selectOne(any(Wrapper.class))).thenReturn(null, new FlowModel());
        assertThrows(BizException.class, () -> engine.startInstanceByCode("leave", "request", actor));
        assertThrows(BizException.class, () -> engine.startInstanceByCode("leave", "request", actor));
        verifyNoInteractions(runtime);
    }
}
