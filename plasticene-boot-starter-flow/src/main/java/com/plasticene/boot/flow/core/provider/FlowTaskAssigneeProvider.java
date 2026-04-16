package com.plasticene.boot.flow.core.provider;

import com.plasticene.boot.flow.core.model.dto.FlowNode;

import java.util.List;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowTaskAssigneeProvider {

    List<Long> getAssignees(FlowNode currentNode);

    Long getLeader(Long userId);

    Long getDeptLeader(Long userId);
}
