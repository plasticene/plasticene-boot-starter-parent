package com.plasticene.boot.flow.core.event;

import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 审批任务事件的只读快照。
 *
 * @param eventId 事件唯一标识，重复投递保持不变。
 * @param eventType 事件类型。
 * @param eventTime 事件发生时间。
 * @param orgId 所属租户ID。
 * @param instanceId 流程实例ID。
 * @param definitionId 流程发布定义ID。
 * @param businessId 关联业务ID，可为空。
 * @param category 流程分类编码。
 * @param taskId 审批任务ID。
 * @param nodeKey 审批节点标识。
 * @param nodeName 审批节点名称。
 * @param assignee 任务处理人ID。
 * @param taskStatus 任务业务状态；等待加签为等待状态，其他取消事件为取消状态。
 * @param action 完成动作，仅任务完成事件有值。
 * @param cancelReason 失效原因，仅任务取消事件有值。
 *
 * @author ZFJ
 * @since 2026-11-06
 */
public record FlowTaskEvent(
        String eventId,
        FlowEventType eventType,
        LocalDateTime eventTime,
        Long orgId,
        Long instanceId,
        Long definitionId,
        Long businessId,
        String category,
        Long taskId,
        String nodeKey,
        String nodeName,
        Long assignee,
        Integer taskStatus,
        FlowTaskAction action,
        FlowTaskCancelReason cancelReason) implements FlowEvent {

    public static FlowTaskEvent of(FlowInstance instance, FlowTask task, FlowEventType type,
                                   Integer taskStatus, FlowTaskAction action, FlowTaskCancelReason reason) {
        return new FlowTaskEvent(UUID.randomUUID().toString(), type, LocalDateTime.now(),
                instance.getOrgId(), instance.getId(), instance.getDefinitionId(), instance.getBusinessId(),
                instance.getCategory(), task.getId(), task.getNodeKey(), task.getNodeName(), task.getAssignee(),
                taskStatus, action, reason);
    }
}
