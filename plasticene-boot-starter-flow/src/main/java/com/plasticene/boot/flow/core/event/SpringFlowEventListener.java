package com.plasticene.boot.flow.core.event;

import org.springframework.context.event.EventListener;

import java.util.Objects;

/**
 * 默认 Spring 发布方式下，按流程分类匹配的流程事件监听器抽象。
 * @author ZFJ
 * @since 2026-11-06
 */
public abstract class SpringFlowEventListener {

    @EventListener
    public void onApplicationEvent(FlowEvent event) {
        if (!Objects.equals(businessType(), event.category())) {
            return;
        }
        doProcessEvent(event);
    }

    /**
     * 处理分类匹配的实例或任务事件。
     * @param event 流程事件
     */
    public abstract void doProcessEvent(FlowEvent event);

    /**
     * 监听器业务类型，与流程分类编码一致。
     * @return 业务类型
     */
    public abstract String businessType();
}
