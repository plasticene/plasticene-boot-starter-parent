package com.plasticene.boot.example.flow.leave;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.plasticene.boot.flow.autoconfigure.FlowEventProperties;
import com.plasticene.boot.flow.core.FlowEngine;
import com.plasticene.boot.flow.core.dao.FlowEventOutboxDAO;
import com.plasticene.boot.flow.core.dao.FlowInstanceDAO;
import com.plasticene.boot.flow.core.event.FlowEventDispatcher;
import com.plasticene.boot.flow.core.event.FlowEventStore;
import com.plasticene.boot.flow.core.event.FlowInstanceEvent;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 在显式授权的专用 MySQL 测试库验证审批事务和可靠事件闭环。
 * @author ZFJ
 * @since 2026-11-11
 */
@EnabledIfEnvironmentVariable(named = "FLOW_LEAVE_TEST_DB", matches = "true")
@EnabledIfEnvironmentVariable(named = "FLOW_LEAVE_JDBC_URL", matches = "jdbc:mysql://[^/]+/[A-Za-z0-9_]+_flow_test(?:\\?.*)?")
@SpringBootTest(classes = LeaveExampleApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.profiles.active=leave", "ptc.flow.event.enabled=false",
                "leave.demo.enabled=false", "leave.business.scheduling-enabled=false"})
class LeaveWorkflowIntegrationTest {
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager manager;
    @Autowired private FlowEngine engine;
    @Autowired private FlowEventStore events;
    @Autowired private FlowEventOutboxDAO eventDAO;
    @Autowired private FlowInstanceDAO instanceDAO;
    @Autowired private FlowEventDispatcher dispatcher;
    @Autowired private FlowEventProperties properties;
    @Autowired private LeaveApplicationService applications;
    @Autowired private LeaveEventReceiver receiver;
    @Autowired private LeaveBusinessProcessor business;

    @BeforeEach
    void initializeDedicatedTestDatabase() throws Exception {
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource(System.getProperty("flow.example.schema",
                    "../../plasticene-boot-starter-flow/docs/sql/leave-example.sql")));
        }
        // 仅在上方显式指定的专用测试库清理演示组织，不接触其他组织。
        for (String table : new String[]{"flow_event_outbox", "flow_task", "flow_instance",
                "leave_event_inbox", "leave_notification", "leave_application"}) {
            jdbc.update("DELETE FROM " + table + " WHERE org_id = 1");
        }
        assertNull(com.plasticene.boot.common.user.LoginUserHolder.get());
    }

    @Test
    void shouldSubmitApproveReceiveAndApplyWithoutHttpContext() throws Exception {
        String request = UUID.randomUUID().toString();
        Long id = applications.submit(request, 2, "请假", LeaveDemoRunner.actor(1L));
        assertEquals(id, applications.submit(request, 2, "重复请求", LeaveDemoRunner.actor(1L)));
        FlowTaskParam task = new FlowTaskParam();
        task.setTaskId(jdbc.queryForObject("SELECT id FROM flow_task WHERE instance_id = ? AND assignee = 2 AND status = 0", Long.class, id));
        task.setComment("同意");
        engine.approveTask(task, LeaveDemoRunner.actor(2L));
        assertEquals(1, jdbc.queryForObject("SELECT status FROM flow_instance WHERE id = ?", Integer.class, id));
        assertEquals("SUBMITTED", applicationState(request));
        drain();
        assertEquals("SUBMITTED", applicationState(request));
        applyBusiness();
        assertEquals("EFFECTIVE", applicationState(request));
        String payload = jdbc.queryForObject("SELECT payload FROM flow_event_outbox WHERE instance_id = ? AND event_type = 'INSTANCE_APPROVED'", String.class, id);
        receiver.receive(JsonMapper.builder().addModule(new JavaTimeModule()).build().readValue(payload, FlowInstanceEvent.class));
        applyBusiness();
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM leave_notification WHERE org_id = 1 AND application_id = ?", Integer.class, request));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM leave_notification WHERE org_id = 1 AND application_id = ? AND sent = TRUE", Integer.class, request));
    }

    @Test
    void shouldRollbackBusinessInstanceTasksAndEventsTogether() {
        String request = UUID.randomUUID().toString();
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(manager).execute(status -> {
            applications.submit(request, 2, "请假", LeaveDemoRunner.actor(1L));
            throw new IllegalStateException("业务事务失败");
        }));
        for (String table : new String[]{"leave_application", "flow_instance", "flow_event_outbox"}) {
            String column = table.equals("leave_application") ? "application_id" : "business_id";
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, request));
        }
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM flow_task WHERE org_id = 1", Integer.class));
    }

    @Test
    void shouldCreateOnlyOneInstanceUnderConcurrentStarterSubmissions() throws Exception {
        String request = UUID.randomUUID().toString();
        try (var executor = Executors.newFixedThreadPool(4)) {
            var calls = new ArrayList<java.util.concurrent.Future<Long>>();
            for (int i = 0; i < 4; i++) {
                calls.add(executor.submit(() -> engine.startInstanceByCode("leave-approval",
                        request, Map.of("days", 2), LeaveDemoRunner.actor(1L))));
            }
            Long first = calls.getFirst().get();
            for (var call : calls) {
                assertEquals(first, call.get());
            }
        }
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM flow_instance WHERE business_id = ?", Integer.class, request));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM flow_event_outbox WHERE business_id = ? AND event_type = 'INSTANCE_STARTED'", Integer.class, request));
    }

    @Test
    void shouldRecoverSameEventAfterLeaseExpiryAndRejectStaleAcknowledgement() {
        applications.submit(UUID.randomUUID().toString(), 2, "请假", LeaveDemoRunner.actor(1L));
        var old = events.claim();
        assertNotNull(old);
        var clock = new MutableClock();
        clock.advance(properties.getLease().plusSeconds(1));
        var recovered = store(clock);
        var claim = recovered.claim();
        assertNotNull(claim);
        assertEquals(old.eventId(), claim.eventId());
        assertNotEquals(old.token(), claim.token());
        assertFalse(events.delivered(old));
        assertTrue(recovered.delivered(claim));
    }

    @Test
    void shouldPauseAfterThreeRetriesAndResumeSameEventManually() {
        applications.submit(UUID.randomUUID().toString(), 2, "请假", LeaveDemoRunner.actor(1L));
        var clock = new MutableClock();
        var store = store(clock);
        String eventId = null;
        for (int attempt = 0; attempt <= properties.getMaxRetries(); attempt++) {
            var claim = store.claim();
            assertNotNull(claim);
            if (eventId == null) {
                eventId = claim.eventId();
            }
            assertEquals(eventId, claim.eventId());
            store.failed(claim, new IllegalStateException("接收失败"));
            clock.advance(properties.retryDelay(attempt).plusMillis(1));
        }
        assertEquals("PAUSED", jdbc.queryForObject("SELECT status FROM flow_event_outbox WHERE event_id = ?", String.class, eventId));
        assertNull(store.claim());
        assertFalse(store.retry(2L, eventId));
        assertTrue(store.retry(1L, eventId));
        assertEquals(eventId, store.claim().eventId());
    }

    @Test
    void shouldBlockOnlyTheFailedInstanceAndPreserveItsEventOrder() {
        applications.submit(UUID.randomUUID().toString(), 2, "第一张请假单", LeaveDemoRunner.actor(1L));
        applications.submit(UUID.randomUUID().toString(), 2, "第二张请假单", LeaveDemoRunner.actor(1L));
        var head = events.claim();
        assertNotNull(head);
        Long blocked = jdbc.queryForObject("SELECT instance_id FROM flow_event_outbox WHERE event_id = ?", Long.class, head.eventId());
        events.failed(head, new IllegalStateException("等待重试"));
        var next = events.claim();
        assertNotNull(next);
        assertNotEquals(blocked, jdbc.queryForObject("SELECT instance_id FROM flow_event_outbox WHERE event_id = ?", Long.class, next.eventId()));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM flow_event_outbox WHERE instance_id = ? AND sequence_no > 1 AND status <> 'PENDING'", Integer.class, blocked));
    }

    private String applicationState(String request) {
        return jdbc.queryForObject("SELECT status FROM leave_application WHERE org_id = 1 AND application_id = ?", String.class, request);
    }

    private void drain() {
        for (int i = 0; i < 20 && dispatcher.dispatchOne(); i++) {
            // 当前测试最多产生五个实例及任务事件。
        }
    }

    private void applyBusiness() {
        for (int i = 0; i < 20; i++) {
            business.processPending();
        }
        business.sendPendingNotifications();
    }

    private FlowEventStore store(Clock clock) {
        return new FlowEventStore(dataSource, manager, eventDAO, instanceDAO, JsonMapper.builder().addModule(new JavaTimeModule()).build(), properties, clock);
    }

    /**
     * 推进数据库交付测试的时间，避免等待真实重试间隔。
     * @author ZFJ
     * @since 2026-11-11
     */
    static class MutableClock extends Clock {
        private Instant instant = Instant.now();
        public void advance(Duration duration) {
            instant = instant.plus(duration);
        }
        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }
        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
        @Override
        public Instant instant() {
            return instant;
        }
    }
}
