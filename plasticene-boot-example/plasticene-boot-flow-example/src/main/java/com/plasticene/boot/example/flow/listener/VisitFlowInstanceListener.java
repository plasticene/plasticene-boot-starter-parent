package com.plasticene.boot.example.flow.listener;

import com.plasticene.boot.flow.core.event.FlowEvent;
import com.plasticene.boot.flow.core.event.SpringFlowEventListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ZFJ
 * @date 2025/11/14
 */
@Component
@Slf4j
public class VisitFlowInstanceListener extends SpringFlowEventListener {
    @Override
    public void doProcessEvent(FlowEvent event) {
        log.info("======>>>instance event: {}", event);
    }

    @Override
    public String businessType() {
        return "fw:visit";
    }
}
