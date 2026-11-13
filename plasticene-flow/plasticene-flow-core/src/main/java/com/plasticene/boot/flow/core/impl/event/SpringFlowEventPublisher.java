package com.plasticene.boot.flow.core.impl.event;

import com.plasticene.boot.flow.core.api.event.FlowEvent;

import org.springframework.context.ApplicationEventPublisher;

/**
 * 使用 Spring 应用事件发布流程事件，沿用应用事件广播器的执行及异常处理配置。
 * @author ZFJ
 * @since 2026-11-06
 */
public class SpringFlowEventPublisher implements FlowEventPublisher {

    private final ApplicationEventPublisher publisher;

    public SpringFlowEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(FlowEvent event) {
        publisher.publishEvent(event);
    }
}
