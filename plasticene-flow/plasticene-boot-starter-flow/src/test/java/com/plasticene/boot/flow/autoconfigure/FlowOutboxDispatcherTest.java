package com.plasticene.boot.flow.autoconfigure;

import com.plasticene.boot.flow.core.model.FlowEvent;
import com.plasticene.boot.flow.core.model.FlowOutboxEvent;
import com.plasticene.boot.flow.core.spi.FlowEventHandler;
import com.plasticene.boot.flow.core.spi.FlowRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 流程 Outbox 事件投递与失败重试测试。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
class FlowOutboxDispatcherTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-03T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void marksAClaimedEventAsPublished() {
        FlowOutboxEvent outbox = event();
        RepositoryStub stub = new RepositoryStub(outbox);
        AtomicReference<FlowEvent> delivered = new AtomicReference<>();

        new FlowOutboxDispatcher(stub.repository(), List.of(delivered::set), properties(), CLOCK).poll();

        assertThat(delivered.get()).isEqualTo(outbox.event());
        assertThat(stub.publishedAt).isEqualTo(LocalDateTime.now(CLOCK));
    }

    @Test
    void schedulesRetryWhenAHandlerFails() {
        FlowOutboxEvent outbox = event();
        RepositoryStub stub = new RepositoryStub(outbox);
        FlowEventHandler handler = ignored -> {
            throw new IllegalStateException("downstream unavailable");
        };

        new FlowOutboxDispatcher(stub.repository(), List.of(handler), properties(), CLOCK).poll();

        assertThat(stub.failure).isEqualTo("downstream unavailable");
        assertThat(stub.nextAttemptAt).isEqualTo(LocalDateTime.now(CLOCK).plusSeconds(30));
    }

    private FlowOutboxEvent event() {
        return new FlowOutboxEvent(1L, new FlowEvent("event-1", "tenant-a", "FLOW_INSTANCE_STARTED",
                2L, Map.of(), LocalDateTime.now(CLOCK)), 0);
    }

    private FlowProperties.Outbox properties() {
        FlowProperties.Outbox properties = new FlowProperties.Outbox();
        properties.setRetryInterval(Duration.ofSeconds(30));
        return properties;
    }

    private static final class RepositoryStub {

        private final FlowOutboxEvent outbox;
        private LocalDateTime publishedAt;
        private String failure;
        private LocalDateTime nextAttemptAt;

        private RepositoryStub(FlowOutboxEvent outbox) {
            this.outbox = outbox;
        }

        private FlowRepository repository() {
            return (FlowRepository) Proxy.newProxyInstance(FlowRepository.class.getClassLoader(),
                    new Class<?>[]{FlowRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "findDeliverableEvents" -> List.of(outbox);
                        case "claimEvent" -> true;
                        case "markEventPublished" -> {
                            publishedAt = (LocalDateTime) args[1];
                            yield null;
                        }
                        case "markEventFailed" -> {
                            failure = (String) args[1];
                            nextAttemptAt = (LocalDateTime) args[2];
                            yield null;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
