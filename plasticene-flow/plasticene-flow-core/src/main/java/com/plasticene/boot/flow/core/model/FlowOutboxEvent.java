package com.plasticene.boot.flow.core.model;

/**
 * 等待投递的流程 Outbox 事件及重试次数。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public record FlowOutboxEvent(Long id, FlowEvent event, int attempts) {
}
