package com.plasticene.boot.flow.core.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 流程实例生命周期事件。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public record FlowEvent(String eventId, String tenantId, String eventType, Long instanceId,
                        Map<String, Object> payload, LocalDateTime occurredAt) {

    public FlowEvent {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
