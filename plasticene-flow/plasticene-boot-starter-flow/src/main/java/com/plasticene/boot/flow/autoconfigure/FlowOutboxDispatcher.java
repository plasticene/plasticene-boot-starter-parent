package com.plasticene.boot.flow.autoconfigure;

import com.plasticene.boot.flow.core.model.FlowOutboxEvent;
import com.plasticene.boot.flow.core.spi.FlowEventHandler;
import com.plasticene.boot.flow.core.spi.FlowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 轮询并以至少一次语义投递流程 Outbox 事件。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public class FlowOutboxDispatcher implements InitializingBean, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowOutboxDispatcher.class);

    private final FlowRepository repository;
    private final List<FlowEventHandler> handlers;
    private final FlowProperties.Outbox properties;
    private final Clock clock;
    private ScheduledExecutorService executor;

    public FlowOutboxDispatcher(FlowRepository repository, List<FlowEventHandler> handlers,
                                FlowProperties.Outbox properties, Clock clock) {
        this.repository = repository;
        this.handlers = List.copyOf(handlers);
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public void afterPropertiesSet() {
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "plasticene-flow-outbox");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::pollSafely, properties.getInitialDelay().toMillis(),
                properties.getPollInterval().toMillis(), TimeUnit.MILLISECONDS);
    }

    void poll() {
        LocalDateTime now = LocalDateTime.now(clock);
        for (FlowOutboxEvent outbox : repository.findDeliverableEvents(
                now, properties.getMaxAttempts(), properties.getBatchSize())) {
            if (!repository.claimEvent(outbox.id(), outbox.attempts(), now.plus(properties.getRetryInterval()))) {
                continue;
            }
            try {
                handlers.forEach(handler -> handler.handle(outbox.event()));
                repository.markEventPublished(outbox.id(), LocalDateTime.now(clock));
            } catch (RuntimeException exception) {
                repository.markEventFailed(outbox.id(), exception.getMessage(),
                        LocalDateTime.now(clock).plus(properties.getRetryInterval()));
                LOGGER.warn("Failed to deliver flow event {}, attempt {}", outbox.event().eventId(),
                        outbox.attempts() + 1, exception);
            }
        }
    }

    private void pollSafely() {
        try {
            poll();
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to poll flow outbox events", exception);
        }
    }

    @Override
    public void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
