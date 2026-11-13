package com.plasticene.boot.flow.core.impl.service.instance;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowInstanceDAO;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowDefinition;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowInstance;
import com.plasticene.boot.flow.core.impl.event.FlowEventPublisher;
import com.plasticene.boot.flow.core.impl.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.model.FlowNode;
import com.plasticene.boot.flow.core.impl.service.definition.FlowDefinitionService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证无登录上下文的申请发起及唯一键冲突拒绝。
 * @author ZFJ
 * @since 2026-11-11
 */
class FlowApplicationIdempotencyTest {
    @Test
    void shouldCreateDifferentInstancesForRepeatedWebSubmissions() {
        var fixture = new Fixture();
        when(fixture.definitions.getStartableDefinition(3L, fixture.actor)).thenReturn(fixture.definition);
        when(fixture.instances.insert(any(FlowInstance.class))).thenAnswer(call -> {
            FlowInstance instance = call.getArgument(0);
            instance.setId(21L);
            return 1;
        }).thenAnswer(call -> {
            call.<FlowInstance>getArgument(0).setId(22L);
            return 1;
        });
        assertEquals(21L, fixture.service.startInstance(3L, Map.of("days", 2), fixture.actor));
        assertEquals(22L, fixture.service.startInstance(3L, Map.of("days", 2), fixture.actor));
        var inserted = ArgumentCaptor.forClass(FlowInstance.class);
        verify(fixture.instances, times(2)).insert(inserted.capture());
        assertNull(inserted.getAllValues().get(0).getBusinessId());
        assertNull(inserted.getAllValues().get(1).getBusinessId());
        verify(fixture.executor, times(2)).executeNode(any(), any());
        verify(fixture.publisher, times(2)).publish(argThat(event -> event.businessId() == null));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "request"})
    void shouldInsertTheSuppliedBusinessIdAndPublishAfterCreation(String businessId) {
        var fixture = new Fixture();
        assertNull(LoginUserHolder.get());
        when(fixture.instances.insert(any(FlowInstance.class))).thenAnswer(call -> {
            call.<FlowInstance>getArgument(0).setId(21L);
            return 1;
        });
        assertEquals(21L, fixture.service.startInstance(3L, businessId, Map.of("days", 2), fixture.actor));
        var inserted = ArgumentCaptor.forClass(FlowInstance.class);
        verify(fixture.instances).insert(inserted.capture());
        assertEquals(businessId, inserted.getValue().getBusinessId());
        verifyNoMoreInteractions(fixture.instances);
        var order = inOrder(fixture.instances, fixture.publisher, fixture.executor);
        order.verify(fixture.instances).insert(any(FlowInstance.class));
        order.verify(fixture.publisher).publish(argThat(event -> event.instanceId().equals(21L)));
        order.verify(fixture.executor).executeNode(inserted.getValue(), fixture.definition.getModelNode());
        assertNull(LoginUserHolder.get());
    }

    @Test
    void shouldRejectDuplicateBusinessIdWithoutExecutingOrPublishing() {
        var fixture = new Fixture();
        when(fixture.instances.insert(any(FlowInstance.class)))
                .thenThrow(new DuplicateKeyException("uk_flow_application"));
        var failure = assertThrows(BizException.class,
                () -> fixture.service.startInstance(3L, "request", Map.of(), fixture.actor));
        assertEquals("同一businessId不能重复发起实例", failure.getMessage());
        verify(fixture.instances).insert(any(FlowInstance.class));
        verifyNoMoreInteractions(fixture.instances);
        verifyNoInteractions(fixture.executor, fixture.publisher);
    }

    @Test
    void shouldPropagateOtherInsertFailures() {
        var fixture = new Fixture();
        var failure = new DataIntegrityViolationException("other database constraint");
        when(fixture.instances.insert(any(FlowInstance.class))).thenThrow(failure);
        assertSame(failure, assertThrows(DataIntegrityViolationException.class,
                () -> fixture.service.startInstance(3L, null, Map.of(), fixture.actor)));
        verifyNoInteractions(fixture.executor, fixture.publisher);
    }

    @Test
    void shouldCheckStartPermissionBeforeInserting() {
        var fixture = new Fixture();
        when(fixture.definitions.getStartableDefinition(3L, fixture.actor))
                .thenThrow(new BizException("流程发布定义不存在"));
        assertThrows(BizException.class,
                () -> fixture.service.startInstance(3L, "request", Map.of(), fixture.actor));
        verifyNoInteractions(fixture.instances, fixture.publisher, fixture.executor);
    }

    /**
     * 装配真实发起服务及可控的持久化边界。
     * @author ZFJ
     * @since 2026-11-11
     */
    static class Fixture {
        final FlowRuntimeServiceImpl service = new FlowRuntimeServiceImpl();
        final FlowInstanceDAO instances = mock(FlowInstanceDAO.class);
        final FlowDefinitionService definitions = mock(FlowDefinitionService.class);
        final FlowEventPublisher publisher = mock(FlowEventPublisher.class);
        final ProcessExecutor executor = mock(ProcessExecutor.class);
        final LoginUser actor = new LoginUser();
        final FlowDefinition definition = new FlowDefinition();

        Fixture() {
            actor.setId(8L);
            actor.setOrgId(9L);
            definition.setOrgId(9L);
            definition.setCategory("leave");
            FlowNode node = new FlowNode();
            node.setKey("start");
            definition.setModelNode(node);
            when(definitions.getStartableDefinition(3L, actor)).thenReturn(definition);
            ReflectionTestUtils.setField(service, "flowInstanceDAO", instances);
            ReflectionTestUtils.setField(service, "flowDefinitionService", definitions);
            ReflectionTestUtils.setField(service, "flowEventPublisher", publisher);
            ReflectionTestUtils.setField(service, "processExecutor", executor);
        }
    }
}
