package com.plasticene.boot.flow.core.event;

import com.plasticene.boot.flow.autoconfigure.FlowAutoConfiguration;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;
import org.junit.jupiter.api.Test;
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
    void shouldCreateWorkingSpringPublisherFromAutoConfiguration() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(Subscriber.class);
            context.refresh();
            FlowEventPublisher publisher = new FlowAutoConfiguration().flowEventPublisher(context);
            assertInstanceOf(SpringFlowEventPublisher.class, publisher);
            FlowInstanceEvent event = FlowInstanceEvent.of(instance(), FlowEventType.INSTANCE_STARTED);
            publisher.publish(event);
            assertEquals(List.of(event), context.getBean(Subscriber.class).events);
        }
    }

    @Test
    void shouldDispatchPojoEventsByTypeAndRetainCategoryFiltering() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(Subscriber.class, CategorySubscriber.class);
            context.refresh();
            FlowEventPublisher publisher = new SpringFlowEventPublisher(context);
            FlowInstance instance = instance();
            FlowInstanceEvent started = FlowInstanceEvent.of(instance, FlowEventType.INSTANCE_STARTED);
            publisher.publish(started);
            FlowTaskEvent created = FlowTaskEvent.of(instance, new FlowTask(), FlowEventType.TASK_CREATED, 0, null, null);
            publisher.publish(created);
            instance.setCategory("other");
            publisher.publish(FlowInstanceEvent.of(instance, FlowEventType.INSTANCE_APPROVED));
            assertEquals(3, context.getBean(Subscriber.class).events.size());
            publisher.publish(FlowTaskEvent.of(instance, new FlowTask(), FlowEventType.TASK_CREATED, 0, null, null));
            assertEquals(List.of(started, created), context.getBean(CategorySubscriber.class).events);
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(FlowEventType.class)
    void shouldHandleEveryEventTypeAndFilterOtherCategories(FlowEventType type) {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(CategorySubscriber.class);
            context.refresh();
            FlowEventPublisher publisher = new SpringFlowEventPublisher(context);
            FlowInstance instance = instance();
            FlowEvent matched = event(instance, type);
            publisher.publish(matched);
            instance.setCategory("other");
            publisher.publish(event(instance, type));
            assertEquals(List.of(matched), context.getBean(CategorySubscriber.class).events);
        }
    }

    @Test
    void shouldPropagateListenerFailures() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(FailingSubscriber.class);
            context.refresh();
            var publisher = new SpringFlowEventPublisher(context);
            assertThrows(IllegalStateException.class, () -> publisher.publish(
                    FlowInstanceEvent.of(instance(), FlowEventType.INSTANCE_STARTED)));
        }
    }

    @Test
    void shouldSnapshotInstanceAndTaskInsteadOfRetainingMutableReferences() {
        FlowInstance instance = instance();
        FlowTask task = new FlowTask();
        task.setId(7L);
        task.setAssignee(8L);
        task.setNodeKey("approve");
        FlowInstanceEvent started = FlowInstanceEvent.of(instance, FlowEventType.INSTANCE_STARTED);
        FlowTaskEvent created = FlowTaskEvent.of(instance, task, FlowEventType.TASK_CREATED, 0, null, null);
        instance.setStatus(1);
        instance.setBusinessId(99L);
        task.setId(100L);
        task.setAssignee(101L);
        assertEquals(0, started.status());
        assertEquals(4L, created.businessId());
        assertEquals(7L, created.taskId());
        assertEquals(8L, created.assignee());
        assertEquals(1L, created.orgId());
        assertEquals(2L, created.instanceId());
        assertEquals(3L, created.definitionId());
        assertNotNull(created.eventTime());
        assertNotNull(created.eventId());
        assertNotEquals(started.eventId(), created.eventId());
    }

    private static FlowEvent event(FlowInstance instance, FlowEventType type) {
        return switch (type) {
            case INSTANCE_STARTED, INSTANCE_APPROVED, INSTANCE_REJECTED, INSTANCE_CANCELLED ->
                    FlowInstanceEvent.of(instance, type);
            case TASK_CREATED -> FlowTaskEvent.of(instance, new FlowTask(), type, 0, null, null);
            case TASK_COMPLETED -> FlowTaskEvent.of(instance, new FlowTask(), type, 1, FlowTaskAction.APPROVE, null);
            case TASK_CANCELLED -> FlowTaskEvent.of(instance, new FlowTask(), type, 3, null,
                    FlowTaskCancelReason.INSTANCE_CANCELLED);
        };
    }

    private static FlowInstance instance() {
        FlowInstance instance = new FlowInstance();
        instance.setOrgId(1L);
        instance.setId(2L);
        instance.setDefinitionId(3L);
        instance.setBusinessId(4L);
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
