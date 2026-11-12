package com.plasticene.boot.example.flow.leave;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 由业务侧重试请假生效和通知生成，不修改已提交的流程结果。
 * @author ZFJ
 * @since 2026-11-11
 */
@Profile("leave")
@Component
public class LeaveBusinessProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(LeaveBusinessProcessor.class);
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public LeaveBusinessProcessor(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(manager);
    }

    @Scheduled(fixedDelay = 1000)
    public void processPending() {
        try {
            transactions.executeWithoutResult(status -> {
                var events = jdbc.queryForList("""
                        SELECT * FROM leave_event_inbox WHERE processed = FALSE
                        ORDER BY received_at, event_id LIMIT 1 FOR UPDATE SKIP LOCKED
                        """);
                if (events.isEmpty()) {
                    return;
                }
                var event = events.getFirst();
                String type = (String) event.get("event_type");
                String state = switch (type) {
                    case "INSTANCE_APPROVED" -> "EFFECTIVE";
                    case "INSTANCE_REJECTED" -> "REJECTED";
                    case "INSTANCE_CANCELLED" -> "CANCELLED";
                    default -> null;
                };
                if (state != null) {
                    if (jdbc.update("""
                            UPDATE leave_application SET status = ? WHERE org_id = ?
                                AND application_id = ? AND instance_id = ?
                            """, state, event.get("org_id"), event.get("application_id"), event.get("instance_id")) != 1) {
                        throw new IllegalStateException("请假申请记录不存在，等待业务修复后重试");
                    }
                    jdbc.update("""
                            INSERT INTO leave_notification (event_id, org_id, application_id, recipient_id, message)
                            SELECT ?, org_id, application_id, applicant_id, ? FROM leave_application
                            WHERE org_id = ? AND application_id = ?
                            ON DUPLICATE KEY UPDATE event_id = event_id
                            """, event.get("event_id"), "请假审批结果: " + state,
                            event.get("org_id"), event.get("application_id"));
                }
                jdbc.update("UPDATE leave_event_inbox SET processed = TRUE WHERE event_id = ?", event.get("event_id"));
            });
        } catch (Exception failure) {
            LOG.warn("请假业务处理失败，保留收件记录由业务重试: {}", failure.getClass().getSimpleName());
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void sendPendingNotifications() {
        try {
            transactions.executeWithoutResult(status -> {
                var notifications = jdbc.queryForList("""
                        SELECT * FROM leave_notification WHERE sent = FALSE
                        ORDER BY created_at, event_id LIMIT 1 FOR UPDATE SKIP LOCKED
                        """);
                for (var message : notifications) {
                    // 示例渠道为日志；真实短信/站内信适配及其失败重试由业务替换。
                    LOG.info("请假结果通知 recipientId={} applicationId={} message={}", message.get("recipient_id"),
                            message.get("application_id"), message.get("message"));
                    jdbc.update("UPDATE leave_notification SET sent = TRUE WHERE event_id = ?", message.get("event_id"));
                }
            });
        } catch (Exception failure) {
            LOG.warn("请假通知发送失败，由业务重试: {}", failure.getClass().getSimpleName());
        }
    }
}
