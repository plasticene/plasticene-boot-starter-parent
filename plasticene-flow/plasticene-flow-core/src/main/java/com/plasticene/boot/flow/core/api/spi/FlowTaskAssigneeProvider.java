package com.plasticene.boot.flow.core.api.spi;

import com.plasticene.boot.flow.core.model.FlowNode;
import java.util.List;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowTaskAssigneeProvider {

    default List<Long> getAssignees(FlowNode node, Long orgId, Long applicantId) {
        return List.of();
    }

    default Long getLeader(Long orgId, Long userId) {
        return null;
    }

    default Long getDeptLeader(Long orgId, Long userId) {
        return null;
    }
}
