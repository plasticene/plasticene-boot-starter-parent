package com.plasticene.boot.example.flow.listener;

import com.plasticene.boot.flow.core.api.event.FlowEvent;
import com.plasticene.boot.flow.core.api.event.FlowEventConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ZFJ
 * @date 2025/9/8
 */
@Component
@Slf4j
public class LeaveFlowInstanceListener implements FlowEventConsumer {
    @Override
    public void receive(FlowEvent event) {
        log.info("======>>>instance event: {}", event);
    }

    @Override
    public String businessType() {
        return "fw:leave";
    }
}
