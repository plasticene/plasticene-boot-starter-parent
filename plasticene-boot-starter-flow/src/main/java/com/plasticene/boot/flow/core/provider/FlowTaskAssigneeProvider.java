package com.plasticene.boot.flow.core.provider;

import com.plasticene.boot.flow.core.model.dto.FlowNode;
import java.util.List;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowTaskAssigneeProvider {

    @Deprecated
    default List<Long> getAssignees(FlowNode currentNode) {
        return List.of();
    }

    @Deprecated
    default Long getLeader(Long userId) {
        return null;
    }

    @Deprecated
    default Long getDeptLeader(Long userId) {
        return null;
    }

    default List<Long> getAssignees(FlowNode node, Long orgId, Long applicantId) {
        return getAssignees(node);
    }

    default Long getLeader(Long orgId, Long userId) {
        return getLeader(userId);
    }

    default Long getDeptLeader(Long orgId, Long userId) {
        return getDeptLeader(userId);
    }
}
