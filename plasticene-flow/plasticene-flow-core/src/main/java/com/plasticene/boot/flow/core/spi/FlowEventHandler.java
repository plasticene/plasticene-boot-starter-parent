package com.plasticene.boot.flow.core.spi;

import com.plasticene.boot.flow.core.model.FlowEvent;

/**
 * 接收已提交流程事件的扩展接口。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
@FunctionalInterface
public interface FlowEventHandler {

    void handle(FlowEvent event);
}
