package com.plasticene.boot.example.flow.purchase;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.api.event.FlowEvent;
import com.plasticene.boot.flow.core.api.event.FlowEventType;
import com.plasticene.boot.flow.core.api.event.FlowInstanceEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 验证采购回调重试、终态幂等，以及提交后才输出模拟通知。
 * @author ZFJ
 * @since 2026-11-18
 */
class PurchaseFlowEventConsumerTest {
    private final PurchaseApplyMapper mapper = mock(PurchaseApplyMapper.class);
    private final TestTransactions transactions = new TestTransactions();
    private PurchaseFlowEventConsumer consumer;
    private PurchaseApply apply;
    private final Logger logger = (Logger) LoggerFactory.getLogger(PurchaseFlowEventConsumer.class);
    private final ListAppender<ILoggingEvent> messages = new ListAppender<>();

    @BeforeEach
    void prepare() {
        messages.start();
        logger.addAppender(messages);
        var proxy = new ProxyFactory(new PurchaseFlowEventConsumer(mapper));
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        consumer = (PurchaseFlowEventConsumer) proxy.getProxy();
        apply = new PurchaseApply();
        apply.setId(7L);
        apply.setInstanceId(21L);
        apply.setTitle("电脑采购");
        apply.setAmount(new BigDecimal("5000.00"));
        apply.setStatus(0);
        when(mapper.find(3L, 7L, true)).thenReturn(apply);
        when(mapper.finish(eq(3L), eq(7L), eq(21L), anyInt())).thenReturn(1);
    }

    @AfterEach
    void detachLogger() {
        logger.detachAppender(messages);
        messages.stop();
    }

    private String logged() {
        return messages.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
    }

    @Test
    void shouldNotifyOnlyAfterTransactionCommits() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            consumer.receive(event(FlowEventType.INSTANCE_APPROVED, 1));
            assertFalse(logged().contains("【模拟通知行政】"));
        });
        assertTrue(logged().contains("【模拟通知行政】"));
        verify(mapper).finish(3L, 7L, 21L, 1);
    }

    @Test
    void shouldNotNotifyOnOuterRollback() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            consumer.receive(event(FlowEventType.INSTANCE_APPROVED, 1));
            status.setRollbackOnly();
        });
        assertTrue(transactions.rolledBack);
        assertFalse(logged().contains("【模拟通知行政】"));
    }

    @Test
    void shouldAcknowledgeDuplicatesWithoutUpdatingOrNotifying() {
        apply.setStatus(1);
        consumer.receive(event(FlowEventType.INSTANCE_APPROVED, 1));
        verify(mapper, never()).finish(any(), any(), any(), any());
        assertFalse(logged().contains("【模拟通知行政】"));
    }

    @Test
    void shouldPropagateFailureThenAcceptRetry() {
        when(mapper.finish(3L, 7L, 21L, 1)).thenThrow(new IllegalStateException("模拟数据库失败")).thenReturn(1);
        assertThrows(IllegalStateException.class, () -> consumer.receive(event(FlowEventType.INSTANCE_APPROVED, 1)));
        assertTrue(transactions.rolledBack);
        assertFalse(logged().contains("【模拟通知行政】"));
        consumer.receive(event(FlowEventType.INSTANCE_APPROVED, 1));
        assertEquals(1, logged().split("【模拟通知行政】", -1).length - 1);
    }

    @Test
    void shouldRejectOrCancelWithoutPurchasing() {
        consumer.receive(event(FlowEventType.INSTANCE_REJECTED, 2));
        consumer.receive(event(FlowEventType.INSTANCE_CANCELLED, 3));
        verify(mapper).finish(3L, 7L, 21L, 2);
        verify(mapper).finish(3L, 7L, 21L, 3);
        assertFalse(logged().contains("【模拟通知行政】"));
    }

    @Test
    void shouldRejectAnEventForAnotherInstance() {
        apply.setInstanceId(99L);
        assertThrows(BizException.class, () -> consumer.receive(event(FlowEventType.INSTANCE_APPROVED, 1)));
        verify(mapper, never()).finish(any(), any(), any(), any());
    }

    @Test
    void shouldRejectAnEventFromAnotherOrganization() {
        when(mapper.find(3L, 7L, true)).thenReturn(null);
        assertThrows(BizException.class, () -> consumer.receive(event(FlowEventType.INSTANCE_APPROVED, 1)));
    }

    @Test
    void shouldIgnoreStartTaskEventsAndOldStartReplays() {
        consumer.receive(event(FlowEventType.INSTANCE_STARTED, 0));
        consumer.receive(mock(FlowEvent.class));
        verifyNoInteractions(mapper);
    }

    private FlowInstanceEvent event(FlowEventType type, int status) {
        return new FlowInstanceEvent("event-1", type, LocalDateTime.now(), 3L, 21L, 11L,
                "purchase:7", "purchase", status, 8L);
    }

    /**
     * 只模拟事务边界和提交同步，不连接业务数据库。
     * @author ZFJ
     * @since 2026-11-18
     */
    private static class TestTransactions extends AbstractPlatformTransactionManager {
        private boolean active;
        private boolean rolledBack;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return active;
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            active = true;
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            active = false;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            active = false;
            rolledBack = true;
        }
    }
}
