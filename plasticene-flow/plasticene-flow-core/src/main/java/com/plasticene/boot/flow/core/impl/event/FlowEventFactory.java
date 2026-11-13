package com.plasticene.boot.flow.core.impl.event;

import com.plasticene.boot.flow.core.api.event.FlowEventType;
import com.plasticene.boot.flow.core.api.event.FlowInstanceEvent;
import com.plasticene.boot.flow.core.api.event.FlowTaskAction;
import com.plasticene.boot.flow.core.api.event.FlowTaskCancelReason;
import com.plasticene.boot.flow.core.api.event.FlowTaskEvent;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowInstance;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTask;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 将流程持久化状态转换为对外事件快照。
 * @author ZFJ
 * @since 2026-11-12
 */
public final class FlowEventFactory {

    public static FlowInstanceEvent instanceEvent(FlowInstance instance, FlowEventType type) {
        return new FlowInstanceEvent(UUID.randomUUID().toString(), type, LocalDateTime.now(),
                instance.getOrgId(), instance.getId(), instance.getDefinitionId(), instance.getBusinessId(),
                instance.getCategory(), instance.getStatus(), instance.getUserId());
    }

    public static FlowTaskEvent taskEvent(FlowInstance instance, FlowTask task, FlowEventType type,
                                         Integer taskStatus, FlowTaskAction action, FlowTaskCancelReason reason) {
        return new FlowTaskEvent(UUID.randomUUID().toString(), type, LocalDateTime.now(),
                instance.getOrgId(), instance.getId(), instance.getDefinitionId(), instance.getBusinessId(),
                instance.getCategory(), task.getId(), task.getNodeKey(), task.getNodeName(), task.getAssignee(),
                taskStatus, action, reason);
    }

    private FlowEventFactory() {
    }
}
