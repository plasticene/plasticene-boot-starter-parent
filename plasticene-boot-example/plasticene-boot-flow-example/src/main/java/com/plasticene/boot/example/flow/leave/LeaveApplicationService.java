package com.plasticene.boot.example.flow.leave;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.FlowEngine;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

/**
 * 同事务保存业务请假申请并按稳定流程编码发起审批。
 * @author ZFJ
 * @since 2026-11-11
 */
@Profile("leave")
@Service
public class LeaveApplicationService {
    private final JdbcTemplate jdbc;
    private final FlowEngine engine;

    public LeaveApplicationService(JdbcTemplate jdbc, FlowEngine engine) {
        this.jdbc = jdbc;
        this.engine = engine;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long submit(String applicationId, int days, String reason, LoginUser actor) {
        com.plasticene.boot.flow.core.utils.FlowActors.require(actor);
        if (applicationId == null || applicationId.isBlank() || days <= 0 || reason == null || reason.isBlank()) {
            throw new BizException("申请标识、正数请假天数及原因不能为空");
        }
        jdbc.update("""
                INSERT INTO leave_application (org_id, application_id, applicant_id, days, reason, status)
                VALUES (?, ?, ?, ?, ?, 'SUBMITTED') ON DUPLICATE KEY UPDATE application_id = application_id
                """, actor.getOrgId(), applicationId, actor.getId(), days, reason);
        var saved = jdbc.queryForMap("SELECT * FROM leave_application WHERE org_id = ? AND application_id = ? FOR UPDATE",
                actor.getOrgId(), applicationId);
        if (((Number) saved.get("applicant_id")).longValue() != actor.getId()) {
            throw new BizException("无权重复提交他人的申请");
        }
        Long id = engine.startInstanceByCode("leave-approval", applicationId,
                Map.of("days", saved.get("days"), "reason", saved.get("reason")), actor);
        jdbc.update("""
                UPDATE leave_application SET instance_id = ? WHERE org_id = ? AND application_id = ?
                    AND instance_id IS NULL
                """, id, actor.getOrgId(), applicationId);
        return id;
    }
}
