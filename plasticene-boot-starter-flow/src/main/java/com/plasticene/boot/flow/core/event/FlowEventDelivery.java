package com.plasticene.boot.flow.core.event;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 流程事件交付状态及失败诊断，不暴露事件载荷。
 * @author ZFJ
 * @since 2026-11-11
 */
@Schema(description = "流程事件交付状态")
public record FlowEventDelivery(
        @Schema(description = "事件唯一标识") String eventId,
        @Schema(description = "所属组织ID") Long orgId,
        @Schema(description = "流程实例ID") Long instanceId,
        @Schema(description = "流程分类编码") String category,
        @Schema(description = "事件类型") String eventType,
        @Schema(description = "交付状态") String status,
        @Schema(description = "本轮失败重试计数") int retryCount,
        @Schema(description = "最近失败原因") String lastError,
        @Schema(description = "下次投递时间") LocalDateTime nextAttemptAt) {
}
