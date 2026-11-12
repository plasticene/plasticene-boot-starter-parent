package com.plasticene.boot.flow.core.event;



/**
 * 按业务分类接收已提交的流程事件，正常返回即确认接收。
 * @author ZFJ
 * @since 2026-11-11
 */
public interface FlowEventConsumer {
    String businessType();

    void receive(FlowEvent event) throws Exception;
}
