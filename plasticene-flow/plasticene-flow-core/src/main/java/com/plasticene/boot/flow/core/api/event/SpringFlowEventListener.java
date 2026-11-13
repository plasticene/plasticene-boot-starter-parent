package com.plasticene.boot.flow.core.api.event;

/**
 * 兼容原业务监听器，分类路由统一由事件接收方注册表负责。
 * @author ZFJ
 * @since 2026-11-06
 */
public abstract class SpringFlowEventListener implements FlowEventConsumer {

    /**
     * 处理分类匹配的实例或任务事件。
     * @param event 流程事件
     */
    @Override
    public void receive(FlowEvent event) {
        doProcessEvent(event);
    }

    public abstract void doProcessEvent(FlowEvent event);

    /**
     * 监听器业务类型，与流程分类编码一致。
     * @return 业务类型
     */
    public abstract String businessType();
}
