package com.plasticene.boot.flow.core.impl.event;

import com.plasticene.boot.flow.core.api.event.FlowEvent;
import com.plasticene.boot.flow.core.api.event.FlowEventType;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.plasticene.boot.flow.core.config.FlowEventProperties;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowEventOutboxDAO;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowInstanceDAO;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowEventOutbox;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowInstance;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 验证 MyBatis 事件存储保留发布事务、独立领取事务及失败恢复边界。
 * @author ZFJ
 * @since 2026-11-12
 */
class FlowEventStoreTest {
    private final FlowEventOutboxDAO events = mock(FlowEventOutboxDAO.class);
    private final FlowInstanceDAO instances = mock(FlowInstanceDAO.class);
    private final DataSource dataSource = mock(DataSource.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-12T00:00:00Z"), ZoneOffset.UTC);
    private final FlowEventProperties properties = new FlowEventProperties();
    private final FlowEventStore.Claimed claim = new FlowEventStore.Claimed("e", "INSTANCE_STARTED", "{}", "token");
    private Connection connection;
    private DataSourceTransactionManager manager;
    private FlowEventStore store;

    @BeforeEach
    void setup() throws Exception {
        connection = mock(Connection.class);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_READ_COMMITTED);
        when(dataSource.getConnection()).thenReturn(connection);
        manager = new DataSourceTransactionManager(dataSource);
        store = new FlowEventStore(dataSource, manager, events, instances,
                JsonMapper.builder().addModule(new JavaTimeModule()).build(), properties, clock);
    }

    @Test
    void shouldRejectPublishingWithoutTheFlowDatasourceTransaction() {
        assertThrows(IllegalStateException.class, () -> store.publish(event()));
        verifyNoInteractions(events, instances);
    }

    @Test
    void shouldRejectAnActiveTransactionThatDoesNotBindTheFlowDatasource() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertThrows(IllegalStateException.class, () -> store.publish(event()));
            verifyNoInteractions(events, instances);
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"BIZ-1"})
    void shouldPersistSequenceAndPayloadInsideTheHostTransactionAndRollbackTogether(String businessId) throws Exception {
        when(instances.incrementEventSequence(1L, 2L)).thenReturn(1);
        when(instances.selectEventSequence(1L, 2L)).thenReturn(7L);
        FlowEvent event = event(businessId);
        doAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertTrue(TransactionSynchronizationManager.hasResource(dataSource));
            verify(connection, never()).commit();
            return 1;
        }).when(events).insertEvent(any());
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(manager).execute(status -> {
            store.publish(event);
            throw new IllegalStateException("业务回滚");
        }));
        var record = ArgumentCaptor.forClass(FlowEventOutbox.class);
        var order = inOrder(instances, events);
        order.verify(instances).incrementEventSequence(1L, 2L);
        order.verify(instances).selectEventSequence(1L, 2L);
        order.verify(events).insertEvent(record.capture());
        assertEquals(businessId, record.getValue().getBusinessId());
        assertEquals(7L, record.getValue().getSequenceNo());
        assertEquals(event.eventId(), record.getValue().getEventId());
        assertEquals("PENDING", record.getValue().getStatus());
        assertEquals(event, store.decode(new FlowEventStore.Claimed(event.eventId(), event.eventType().name(),
                record.getValue().getPayload(), "token")));
        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    @Test
    void shouldRollbackWhenTheEventInsertFails() throws Exception {
        when(instances.incrementEventSequence(1L, 2L)).thenReturn(1);
        when(instances.selectEventSequence(1L, 2L)).thenReturn(7L);
        doThrow(new IllegalStateException("写入失败")).when(events).insertEvent(any());
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(manager).execute(status -> {
            store.publish(event());
            return null;
        }));
        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    @Test
    void shouldClaimInANewTransactionAndResumeTheHostTransaction() throws Exception {
        Connection worker = mock(Connection.class);
        when(worker.getAutoCommit()).thenReturn(true);
        when(worker.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_READ_COMMITTED);
        when(dataSource.getConnection()).thenReturn(connection, worker);
        var candidate = new FlowEventOutbox();
        candidate.setEventId("e");
        candidate.setEventType("INSTANCE_STARTED");
        candidate.setPayload("{}");
        when(events.selectClaimCandidate(any())).thenAnswer(invocation -> {
            var holder = (ConnectionHolder)
                    TransactionSynchronizationManager.getResource(dataSource);
            assertNotNull(holder);
            assertSame(worker, holder.getConnection());
            assertEquals(Connection.TRANSACTION_READ_COMMITTED,
                    TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
            return candidate;
        });
        var claimed = new TransactionTemplate(manager).execute(status -> {
            var result = store.claim();
            var holder = (ConnectionHolder)
                    TransactionSynchronizationManager.getResource(dataSource);
            assertNotNull(holder);
            assertSame(connection, holder.getConnection());
            return result;
        });
        assertNotNull(claimed);
        assertEquals("e", claimed.eventId());
        verify(events).markProcessing(eq("e"), eq(claimed.token()), eq(now().plus(properties.getLease())));
        verify(worker).commit();
        verify(connection).commit();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void shouldPauseOnlyAfterTheInitialAttemptAndThreeRetries(int used) {
        when(events.selectRetryCountForUpdate(eq("e"), eq("token"), any())).thenReturn(used);
        store.failed(claim, new IllegalStateException("接收失败"));
        verify(events).markFailed("e", "token", used == 3 ? "PAUSED" : "PENDING", used + 1,
                now().plus(properties.retryDelay(used)), "IllegalStateException: 接收失败");
    }

    @Test
    void shouldNotRecordFailureAfterLosingTheLease() {
        when(events.selectRetryCountForUpdate(eq("e"), eq("token"), any())).thenReturn(null);
        store.failed(claim, new IllegalStateException("旧工作者"));
        verify(events, never()).markFailed(anyString(), anyString(), anyString(), anyInt(), any(), anyString());
    }

    @Test
    void shouldReportLostLeaseInsteadOfAcknowledgingOrRenewing() {
        assertFalse(store.delivered(claim));
        assertFalse(store.renew(claim));
        verify(events).markDelivered("e", "token", now());
        verify(events).renewLease("e", "token", now().plus(properties.getLease()), now());
    }

    private FlowEvent event() {
        return event("BIZ-1");
    }

    private FlowEvent event(String businessId) {
        var instance = new FlowInstance();
        instance.setOrgId(1L);
        instance.setId(2L);
        instance.setDefinitionId(3L);
        instance.setCategory("leave");
        instance.setBusinessId(businessId);
        return FlowEventFactory.instanceEvent(instance, FlowEventType.INSTANCE_STARTED);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
