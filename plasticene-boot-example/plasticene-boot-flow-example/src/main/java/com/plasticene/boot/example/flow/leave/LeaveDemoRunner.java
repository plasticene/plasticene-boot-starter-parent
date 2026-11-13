package com.plasticene.boot.example.flow.leave;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.FlowEngine;
import com.plasticene.boot.flow.core.api.param.FlowTaskParam;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.UUID;

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
    private final FlowEngine engine;
    private final JdbcTemplate jdbc;

    public LeaveDemoRunner(LeaveApplicationService applications, FlowEngine engine, JdbcTemplate jdbc) {
        this.applications = applications;
        this.engine = engine;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String applicationId = UUID.randomUUID().toString();
        LoginUser applicant = actor(1L);
        Long instanceId = applications.submit(applicationId, 2, "示例请假", applicant);
        if (!instanceId.equals(applications.submit(applicationId, 2, "示例请假", applicant))) {
            throw new IllegalStateException("重复提交没有复用实例");
        }
        Long taskId = jdbc.queryForObject("SELECT id FROM flow_task WHERE instance_id = ? AND assignee = 2 AND status = 0 LIMIT 1", Long.class, instanceId);
        FlowTaskParam param = new FlowTaskParam();
        param.setTaskId(taskId);
        param.setComment("同意请假");
        engine.approveTask(param, actor(2L));
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(30).toNanos();
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
        actor.setRoleIds(java.util.List.of());
        return actor;
    }
}
