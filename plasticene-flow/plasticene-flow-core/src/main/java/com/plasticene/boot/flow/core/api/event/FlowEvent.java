package com.plasticene.boot.flow.core.api.event;

import java.time.LocalDateTime;

/**
 * 流程事件公共契约，独立于具体通知机制。
 * @author ZFJ
 * @since 2026-11-06
 */
public interface FlowEvent {
    /** 事件唯一标识，重复投递保持不变。 */
    String eventId();

    /** 事件类型。 */
    FlowEventType eventType();

    /** 事件发生时间。 */
    LocalDateTime eventTime();

    /** 所属租户ID。 */
    Long orgId();

    /** 流程实例ID。 */
    Long instanceId();

    /** 流程发布定义ID。 */
    Long definitionId();

    /** 关联业务ID，可为空。 */
    String businessId();

    /** 流程分类编码。 */
    String category();

}
