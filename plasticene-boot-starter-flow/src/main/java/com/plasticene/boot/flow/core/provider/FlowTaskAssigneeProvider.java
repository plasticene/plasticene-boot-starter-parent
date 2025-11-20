package com.plasticene.boot.flow.core.provider;

import com.plasticene.boot.flow.core.model.dto.ProcessNode;

import java.util.List;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowTaskAssigneeProvider {

    List<Long> getAssignees(ProcessNode currentNode);
}
