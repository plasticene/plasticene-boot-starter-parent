package com.plasticene.boot.flow.core.impl.event;

import com.plasticene.boot.flow.core.api.event.FlowEvent;
import com.plasticene.boot.flow.core.api.event.FlowEventConsumer;
import com.plasticene.boot.flow.core.api.event.FlowEventType;

import com.plasticene.boot.flow.core.config.FlowEventProperties;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowInstance;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证可靠事件交付确认、异常、超时和指数退避边界。
 * @author ZFJ
 * @since 2026-11-11
 */
class FlowEventDeliveryTest {
    @Test
    void shouldUseThreeRetriesWithCappedExponentialDelay() {
        var properties = new FlowEventProperties();
        properties.validate();
        assertEquals(3, properties.getMaxRetries());
        assertEquals(Duration.ofSeconds(30), properties.retryDelay(0));
        assertEquals(Duration.ofSeconds(60), properties.retryDelay(1));
        assertEquals(Duration.ofSeconds(120), properties.retryDelay(2));
        assertEquals(Duration.ofMinutes(30), properties.retryDelay(1000));
        properties.setInitialDelay(Duration.ofDays(365));
        assertEquals(Duration.ofMinutes(30), properties.retryDelay(0));
    }

    @Test
    void shouldOnlyAcknowledgeAfterBusinessReturns() throws Exception {
        var store = mock(FlowEventStore.class);
        var claim = new FlowEventStore.Claimed("e", "INSTANCE_APPROVED", "{}", "lease");
        FlowInstance instance = new FlowInstance();
        instance.setCategory("leave");
        var event = FlowEventFactory.instanceEvent(instance, FlowEventType.INSTANCE_APPROVED);
        when(store.claim()).thenReturn(claim);
        when(store.decode(claim)).thenReturn(event);
        AtomicInteger received = new AtomicInteger();
        var consumer = new FlowEventConsumer() {
            public String businessType() {
                return "LEAVE";
            }
            public void receive(FlowEvent value) {
                assertSame(event, value);
                verify(store, never()).delivered(claim);
                received.incrementAndGet();
            }
        };
        var dispatcher = new FlowEventDispatcher(store, new FlowEventConsumers(List.of(consumer)), new FlowEventProperties());
        try {
            assertTrue(dispatcher.dispatchOne());
            verify(store).delivered(claim);
            verify(store, never()).failed(any(), any());
            assertEquals(1, received.get());
        } finally {
            dispatcher.stop();
        }
    }

    @Test
    void shouldRetryMissingReceiversInsteadOfAcknowledging() {
        var store = mock(FlowEventStore.class);
        var claim = new FlowEventStore.Claimed("e", "INSTANCE_APPROVED", "{}", "lease");
        when(store.claim()).thenReturn(claim);
        var dispatcher = new FlowEventDispatcher(store, new FlowEventConsumers(List.of()), new FlowEventProperties());
        try {
            assertTrue(dispatcher.dispatchOne());
            verify(store).failed(eq(claim), any());
            verify(store, never()).delivered(any());
        } finally {
            dispatcher.stop();
        }
    }

    @Test
    void shouldTimeOutBusinessReceiverWithoutAcknowledging() throws Exception {
        var store = mock(FlowEventStore.class);
        var claim = new FlowEventStore.Claimed("e", "INSTANCE_APPROVED", "{}", "lease");
        FlowInstance instance = new FlowInstance();
        instance.setCategory("leave");
        when(store.claim()).thenReturn(claim);
        when(store.decode(claim)).thenReturn(FlowEventFactory.instanceEvent(instance, FlowEventType.INSTANCE_APPROVED));
        var consumer = new FlowEventConsumer() {
            public String businessType() {
                return "leave";
            }
            public void receive(FlowEvent event) throws InterruptedException {
                new CountDownLatch(1).await();
            }
        };
        var properties = new FlowEventProperties();
        properties.setCallbackTimeout(Duration.ofMillis(50));
        var dispatcher = new FlowEventDispatcher(store, new FlowEventConsumers(List.of(consumer)), properties);
        try {
            assertTrue(dispatcher.dispatchOne());
            verify(store).failed(eq(claim), isA(TimeoutException.class));
            verify(store, never()).delivered(any());
        } finally {
            dispatcher.stop();
        }
    }
}
