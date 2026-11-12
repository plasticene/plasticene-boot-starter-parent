package com.plasticene.boot.example.flow.leave;

import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.provider.DefaultFlowTaskAssigneeProvider;
import com.plasticene.boot.flow.core.provider.FlowOrganizationProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/**
 * 为演示组织提供员工和部门负责人，不读取登录请求。
 * @author ZFJ
 * @since 2026-11-11
 */
@Profile("leave")
@Component
public class LeaveOrganization extends DefaultFlowTaskAssigneeProvider implements FlowOrganizationProvider {
    @Override
    public List<Long> getAssignees(FlowNode node, Long orgId, Long applicantId) {
        if ("department-approval".equals(node.getKey())) {
            return List.of(getDeptLeader(orgId, applicantId));
        }
        return super.getAssignees(node, orgId, applicantId);
    }

    @Override
    public Long getDeptLeader(Long orgId, Long userId) {
        return 2L;
    }

    @Override
    public Map<Long, String> getUserMap(Long orgId) {
        return Map.of(1L, "示例员工", 2L, "示例部门负责人");
    }

    @Override
    public Map<Long, String> getActiveUserMap(Long orgId) {
        return getUserMap(orgId);
    }
}
