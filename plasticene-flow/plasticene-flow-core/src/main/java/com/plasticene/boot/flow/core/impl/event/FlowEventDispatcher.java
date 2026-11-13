package com.plasticene.boot.flow.core.impl.event;

import com.plasticene.boot.flow.core.config.FlowEventProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 在独立线程中领取、交付及确认流程事件。
 * @author ZFJ
 * @since 2026-11-11
 */
public class FlowEventDispatcher implements SmartLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(FlowEventDispatcher.class);
    private final FlowEventStore store;
    private final FlowEventConsumers consumers;
    private final FlowEventProperties properties;
    private final ThreadPoolTaskScheduler scheduler;
    private final ThreadPoolExecutor workers;
    private final ThreadPoolExecutor callbacks;
    private volatile boolean running;
    private ScheduledFuture<?> polling;
    private ScheduledFuture<?> cleanup;

    public FlowEventDispatcher(FlowEventStore store, FlowEventConsumers consumers, FlowEventProperties properties) {
        this.store = store;
        this.consumers = consumers;
        this.properties = properties;
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("flow-event-scheduler-");
        scheduler.setDaemon(true);
        scheduler.initialize();
        workers = pool("flow-event-worker-");
        callbacks = pool("flow-event-consumer-");
    }

    @Override
    public synchronized void start() {
        if (running || !properties.isEnabled()) {
            return;
        }
        running = true;
        polling = scheduler.scheduleWithFixedDelay(this::poll, properties.getPollInterval());
        cleanup = scheduler.scheduleWithFixedDelay(() -> {
            try {
                store.cleanup();
            } catch (Exception failure) {
                LOG.warn("流程事件清理失败", failure);
            }
        }, Instant.now().plusSeconds(3600), Duration.ofHours(1));
    }

    public boolean dispatchOne() {
        FlowEventStore.Claimed event = store.claim();
        if (event == null) {
            return false;
        }
        ScheduledFuture<?> heartbeat = null;
        Future<?> callback = null;
        try {
            heartbeat = scheduler.scheduleAtFixedRate(() -> store.renew(event),
                    properties.getLease().dividedBy(3));
            callback = callbacks.submit(() -> {
                try {
                    consumers.deliver(store.decode(event));
                } catch (Exception failure) {
                    throw new CompletionException(failure);
                }
            });
            callback.get(properties.getCallbackTimeout().toMillis(), TimeUnit.MILLISECONDS);
            store.delivered(event);
        } catch (Exception failure) {
            if (callback != null) {
                callback.cancel(true);
            }
            if (failure instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            Throwable cause = failure;
            while (cause.getCause() != null && (cause instanceof ExecutionException
                    || cause instanceof CompletionException)) {
                cause = cause.getCause();
            }
            store.failed(event, cause);
        } finally {
            if (heartbeat != null) {
                heartbeat.cancel(false);
            }
        }
        return true;
    }

    @Override
    public synchronized void stop() {
        running = false;
        if (polling != null) {
            polling.cancel(false);
        }
        if (cleanup != null) {
            cleanup.cancel(false);
        }
        workers.shutdownNow();
        callbacks.shutdownNow();
        scheduler.shutdown();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void poll() {
        for (int i = 0; i < properties.getWorkers() && running; i++) {
            try {
                workers.execute(() -> {
                    try {
                        dispatchOne();
                    } catch (Exception failure) {
                        LOG.warn("流程事件投递任务失败", failure);
                    }
                });
            } catch (RejectedExecutionException busy) {
                break;
            }
        }
    }

    private ThreadPoolExecutor pool(String prefix) {
        AtomicInteger sequence = new AtomicInteger();
        return new ThreadPoolExecutor(properties.getWorkers(), properties.getWorkers(), 0,
                TimeUnit.MILLISECONDS, new SynchronousQueue<>(), runnable -> {
                    Thread thread = new Thread(runnable, prefix + sequence.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });
    }
}
