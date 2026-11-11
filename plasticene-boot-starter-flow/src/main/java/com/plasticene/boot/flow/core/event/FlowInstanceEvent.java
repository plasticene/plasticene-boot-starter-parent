package com.plasticene.boot.flow.core.event;

import com.plasticene.boot.flow.core.entity.FlowInstance;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 流程实例事件的只读快照。
 *
 * @param eventId      事件唯一标识，重复投递保持不变。
 * @param eventType    事件类型。
 * @param eventTime    事件发生时间。
 * @param orgId        所属租户ID。
 * @param instanceId   流程实例ID。
 * @param definitionId 流程发布定义ID。
 * @param businessId   关联业务ID，可为空。
 * @param category     流程分类编码。
 * @param status       实例状态。
 * @param startUserId  流程发起人ID
 *                     。
 * @author ZFJ
 * @since 2026-11-06
 */
public record FlowInstanceEvent(String eventId, FlowEventType eventType, LocalDateTime eventTime, Long orgId,
                                Long instanceId, Long definitionId, String businessId, String category, Integer status,
                                Long startUserId) implements FlowEvent {
    public static FlowInstanceEvent of(FlowInstance instance, FlowEventType type) {
        return new FlowInstanceEvent(UUID.randomUUID().toString(), type, LocalDateTime.now(),
                instance.getOrgId(), instance.getId(), instance.getDefinitionId(), instance.getBusinessId(),
                instance.getCategory(), instance.getStatus(), instance.getUserId());
    }
}
