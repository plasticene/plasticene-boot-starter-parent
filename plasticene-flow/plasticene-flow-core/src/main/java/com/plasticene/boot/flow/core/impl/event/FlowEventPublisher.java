package com.plasticene.boot.flow.core.impl.event;

import com.plasticene.boot.flow.core.api.event.FlowEvent;

/**
 * 流程事件发布出口，由具体实现决定通知机制。
 * @author ZFJ
 * @since 2026-11-06
 */
public interface FlowEventPublisher {

    /**
     * 发布事件；执行时机和交付保证由实现约定。
     * @param event 事件
     */
    void publish(FlowEvent event);
}
