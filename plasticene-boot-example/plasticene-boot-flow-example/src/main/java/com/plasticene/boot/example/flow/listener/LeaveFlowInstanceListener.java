package com.plasticene.boot.example.flow.listener;

import com.plasticene.boot.flow.core.event.BaseFlowInstanceListener;
import com.plasticene.boot.flow.core.event.InstanceEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ZFJ
 * @date 2025/9/8
 */
@Component
@Slf4j
public class LeaveFlowInstanceListener extends BaseFlowInstanceListener {
    @Override
    public void doProcessInstanceEvent(InstanceEvent event) {
        log.info("======>>>instance event {}", event);
    }

    @Override
    public String businessType() {
        return "wf:leave";
    }
}
