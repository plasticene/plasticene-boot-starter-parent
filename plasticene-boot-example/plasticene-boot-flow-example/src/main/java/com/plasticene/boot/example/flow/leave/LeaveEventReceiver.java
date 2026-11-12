package com.plasticene.boot.example.flow.leave;

import com.plasticene.boot.flow.core.event.FlowEvent;
import com.plasticene.boot.flow.core.event.FlowEventConsumer;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 先持久化业务收件记录并去重，提交成功后确认接收流程事件。
 * @author ZFJ
 * @since 2026-11-11
 */
@Profile("leave")
@Component
public class LeaveEventReceiver implements FlowEventConsumer {
    private final JdbcTemplate jdbc;

    public LeaveEventReceiver(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String businessType() {
        return "leave";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(FlowEvent event) {
        jdbc.update("""
                INSERT INTO leave_event_inbox (event_id, org_id, instance_id, application_id, event_type)
                VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE event_id = event_id
                """, event.eventId(), event.orgId(), event.instanceId(), event.businessId(), event.eventType().name());
    }
}
