package com.plasticene.boot.flow.core.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 流程定义的运行实例。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public record FlowInstance(Long id, String tenantId, Long definitionId, String requesterId, String businessKey,
                           FlowEnums.InstanceStatus status, String currentNodeKey, Long formInstanceId,
                           Map<String, Object> variables, LocalDateTime startedAt, LocalDateTime endedAt) {

    public FlowInstance {
        variables = variables == null ? Map.of() : Map.copyOf(variables);
    }

    public FlowInstance withId(Long value) {
        return new FlowInstance(value, tenantId, definitionId, requesterId, businessKey, status,
                currentNodeKey, formInstanceId, variables, startedAt, endedAt);
    }

    public FlowInstance atNode(String nodeKey) {
        return new FlowInstance(id, tenantId, definitionId, requesterId, businessKey, status,
                nodeKey, formInstanceId, variables, startedAt, endedAt);
    }

    public FlowInstance completed(FlowEnums.InstanceStatus finalStatus, String nodeKey, LocalDateTime time) {
        return new FlowInstance(id, tenantId, definitionId, requesterId, businessKey, finalStatus,
                nodeKey, formInstanceId, variables, startedAt, time);
    }

    public FlowInstance resumedAt(String nodeKey) {
        return new FlowInstance(id, tenantId, definitionId, requesterId, businessKey,
                FlowEnums.InstanceStatus.RUNNING, nodeKey, formInstanceId, variables, startedAt, null);
    }
}
