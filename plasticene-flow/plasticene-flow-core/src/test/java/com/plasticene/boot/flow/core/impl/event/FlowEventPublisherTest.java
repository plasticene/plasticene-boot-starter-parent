package com.plasticene.boot.flow.core.impl.event;

import com.plasticene.boot.flow.core.api.event.FlowEvent;
import com.plasticene.boot.flow.core.api.event.FlowEventConsumer;
import com.plasticene.boot.flow.core.api.event.FlowEventType;
import com.plasticene.boot.flow.core.api.event.FlowInstanceEvent;
import com.plasticene.boot.flow.core.api.event.FlowTaskAction;
import com.plasticene.boot.flow.core.api.event.FlowTaskCancelReason;
import com.plasticene.boot.flow.core.api.event.FlowTaskEvent;
import com.plasticene.boot.flow.core.api.event.SpringFlowEventListener;

import com.plasticene.boot.flow.core.impl.persistence.entity.FlowInstance;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证默认事件发布、扩展替换及事件快照语义。
 * @author ZFJ
 * @since 2026-11-06
 */
class FlowEventPublisherTest {

    @Test
    void shouldRejectDuplicateCategoriesIgnoringCase() {
        var first = new CategorySubscriber();
        FlowEventConsumer second = new FlowEventConsumer() {
            public String businessType() {
                return "LEAVE";
            }
            public void receive(FlowEvent event) {
            }
        };
        assertThrows(IllegalArgumentException.class, () -> new FlowEventConsumers(List.of(first, second)));
    }

    @Test
    void shouldFailInsteadOfSilentlyDroppingAnUnmatchedEvent() {
        var consumers = new FlowEventConsumers(List.of());
        assertThrows(IllegalStateException.class, () -> consumers.deliver(
                FlowEventFactory.instanceEvent(instance(), FlowEventType.INSTANCE_STARTED)));
    }

    @ParameterizedTest
    @EnumSource(FlowEventType.class)
    void shouldDeliverAllEventTypesUsingCategoryIgnoringCase(FlowEventType type) throws Exception {
        var subscriber = new CategorySubscriber();
        var consumers = new FlowEventConsumers(List.of(subscriber));
        FlowInstance instance = instance();
        instance.setCategory("LEAVE");
        FlowEvent event = event(instance, type);
        consumers.deliver(event);
        assertEquals(List.of(event), subscriber.events);
    }

    @Test
    void shouldPropagateReceiverFailureForDeliveryRetry() {
        FlowEventConsumer failing = new FlowEventConsumer() {
            public String businessType() {
                return "leave";
            }
            public void receive(FlowEvent event) {
                throw new IllegalStateException("接收失败");
            }
        };
        var consumers = new FlowEventConsumers(List.of(failing));
        assertThrows(IllegalStateException.class, () -> consumers.deliver(
                FlowEventFactory.instanceEvent(instance(), FlowEventType.INSTANCE_STARTED)));
    }

    @Test
    void shouldSnapshotInstanceAndTaskInsteadOfRetainingMutableReferences() {
        FlowInstance instance = instance();
        FlowTask task = new FlowTask();
        task.setId(7L);
        task.setAssignee(8L);
        task.setNodeKey("approve");
        FlowInstanceEvent started = FlowEventFactory.instanceEvent(instance, FlowEventType.INSTANCE_STARTED);
        FlowTaskEvent created = FlowEventFactory.taskEvent(instance, task, FlowEventType.TASK_CREATED, 0, null, null);
        instance.setStatus(1);
        instance.setBusinessId("BIZ-0099");
        task.setId(100L);
        task.setAssignee(101L);
        assertEquals(0, started.status());
        assertEquals("BIZ-0004", started.businessId());
        assertEquals("BIZ-0004", created.businessId());
        assertEquals(7L, created.taskId());
        assertEquals(8L, created.assignee());
        assertEquals(1L, created.orgId());
        assertEquals(2L, created.instanceId());
        assertEquals(3L, created.definitionId());
        assertNotNull(created.eventTime());
        assertNotNull(created.eventId());
        assertNotEquals(started.eventId(), created.eventId());
    }

    @Test
    void shouldAllowEventsWithoutBusinessId() {
        FlowInstance instance = instance();
        instance.setBusinessId(null);
        assertNull(FlowEventFactory.instanceEvent(instance, FlowEventType.INSTANCE_STARTED).businessId());
        assertNull(FlowEventFactory.taskEvent(instance, new FlowTask(), FlowEventType.TASK_CREATED, 0, null, null).businessId());
    }

    private static FlowEvent event(FlowInstance instance, FlowEventType type) {
        return switch (type) {
            case INSTANCE_STARTED, INSTANCE_APPROVED, INSTANCE_REJECTED, INSTANCE_CANCELLED ->
                    FlowEventFactory.instanceEvent(instance, type);
            case TASK_CREATED -> FlowEventFactory.taskEvent(instance, new FlowTask(), type, 0, null, null);
            case TASK_COMPLETED -> FlowEventFactory.taskEvent(instance, new FlowTask(), type, 1, FlowTaskAction.APPROVE, null);
            case TASK_CANCELLED -> FlowEventFactory.taskEvent(instance, new FlowTask(), type, 3, null,
                    FlowTaskCancelReason.INSTANCE_CANCELLED);
        };
    }

    private static FlowInstance instance() {
        FlowInstance instance = new FlowInstance();
        instance.setOrgId(1L);
        instance.setId(2L);
        instance.setDefinitionId(3L);
        instance.setBusinessId("BIZ-0004");
        instance.setCategory("leave");
        instance.setStatus(0);
        instance.setUserId(5L);
        return instance;
    }

    /**
     * 验证公共事件接口订阅。
     * @author ZFJ
     * @since 2026-11-06
     */
    static class Subscriber {
        final List<FlowEvent> events = new ArrayList<>();

        @EventListener
        public void onEvent(FlowEvent event) {
            events.add(event);
        }
    }

    /**
     * 验证流程分类订阅。
     * @author ZFJ
     * @since 2026-11-06
     */
    static class CategorySubscriber extends SpringFlowEventListener {
        final List<FlowEvent> events = new ArrayList<>();

        @Override
        public void doProcessEvent(FlowEvent event) {
            events.add(event);
        }

        @Override
        public String businessType() {
            return "leave";
        }
    }

    /**
     * 验证监听失败传播。
     * @author ZFJ
     * @since 2026-11-06
     */
    static class FailingSubscriber {
        @EventListener
        public void onEvent(FlowEvent event) {
            throw new IllegalStateException("业务处理失败");
        }
    }
}
