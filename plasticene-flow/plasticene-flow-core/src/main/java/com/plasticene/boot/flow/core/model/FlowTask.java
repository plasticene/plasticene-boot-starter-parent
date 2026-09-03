package com.plasticene.boot.flow.core.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程节点生成的审批任务。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public record FlowTask(Long id, String tenantId, Long instanceId, String nodeKey, String nodeName,
                       String assigneeId, List<String> approvalOrder, int orderIndex,
                       FlowEnums.ApprovalMode approvalMode, FlowEnums.TaskStatus status,
                       boolean requireComment, String comment, LocalDateTime startedAt, LocalDateTime endedAt) {

    public FlowTask {
        approvalOrder = approvalOrder == null ? List.of() : List.copyOf(approvalOrder);
    }

    public FlowTask withId(Long value) {
        return new FlowTask(value, tenantId, instanceId, nodeKey, nodeName, assigneeId, approvalOrder,
                orderIndex, approvalMode, status, requireComment, comment, startedAt, endedAt);
    }

    public FlowTask completed(FlowEnums.TaskStatus finalStatus, String value, LocalDateTime time) {
        return new FlowTask(id, tenantId, instanceId, nodeKey, nodeName, assigneeId, approvalOrder,
                orderIndex, approvalMode, finalStatus, requireComment, value, startedAt, time);
    }

    public FlowTask transferredTo(String newAssigneeId) {
        return new FlowTask(id, tenantId, instanceId, nodeKey, nodeName, newAssigneeId, approvalOrder,
                orderIndex, approvalMode, status, requireComment, comment, startedAt, endedAt);
    }
}
