package com.plasticene.boot.example.flow.leave;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.service.FlowTaskApi;
import com.plasticene.boot.flow.core.api.param.FlowTaskParam;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.UUID;
import com.plasticene.boot.common.exception.BizException;
import java.time.Duration;
import java.util.List;

/**
 * 执行员工提交、负责人审批及异步业务生效的完整演示。
 * @author ZFJ
 * @since 2026-11-11
 */
@Profile("leave")
@Component
@ConditionalOnProperty(name = "leave.demo.enabled", havingValue = "true")
public class LeaveDemoRunner implements ApplicationRunner {
    private final LeaveApplicationService applications;
    private final FlowTaskApi engine;
    private final JdbcTemplate jdbc;

    public LeaveDemoRunner(LeaveApplicationService applications, FlowTaskApi engine, JdbcTemplate jdbc) {
        this.applications = applications;
        this.engine = engine;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String applicationId = UUID.randomUUID().toString();
        LoginUser applicant = actor(1L);
        Long instanceId = applications.submit(applicationId, 2, "示例请假", applicant);
        try {
            applications.submit(applicationId, 2, "示例请假", applicant);
            throw new IllegalStateException("重复申请应被拒绝");
        } catch (BizException expected) {
            if (!"同一businessId不能重复发起实例".equals(expected.getMessage())) { throw expected; }
        }
        Long taskId = jdbc.queryForObject("SELECT id FROM flow_task WHERE instance_id = ? AND assignee = 2 AND status = 0 LIMIT 1", Long.class, instanceId);
        FlowTaskParam param = new FlowTaskParam();
        param.setTaskId(taskId);
        param.setComment("同意请假");
        engine.approve(param, actor(2L));
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline) {
            Integer complete = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM leave_application a JOIN leave_notification n
                        ON n.org_id = a.org_id AND n.application_id = a.application_id
                    WHERE a.org_id = 1 AND a.application_id = ? AND a.status = 'EFFECTIVE' AND n.sent = TRUE
                    """, Integer.class, applicationId);
            if (complete != null && complete > 0) {
                System.out.println("请假审批闭环完成 applicationId=" + applicationId + " instanceId=" + instanceId);
                return;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("请假业务尚未生效，请检查事件投递及业务收件状态");
    }

    public static LoginUser actor(Long userId) {
        LoginUser actor = new LoginUser();
        actor.setId(userId);
        actor.setOrgId(1L);
        actor.setDeptId(1L);
        actor.setRoleIds(List.of());
        return actor;
    }
}
