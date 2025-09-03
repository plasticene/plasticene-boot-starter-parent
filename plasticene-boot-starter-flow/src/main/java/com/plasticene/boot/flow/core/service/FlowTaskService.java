package com.plasticene.boot.flow.core.service;

import com.plasticene.boot.flow.core.dto.ProcessNode;
import com.plasticene.boot.flow.core.dto.ProcessNodeCondition;
import com.plasticene.boot.flow.core.entity.FlowInstance;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowTaskService {

    void createStartTask(FlowInstance instance, ProcessNode currentNode);

    void createApproveTask(FlowInstance instance, ProcessNode currentNode);

    void createCopyTask(FlowInstance instance, ProcessNode currentNode);

    void createEndTask(FlowInstance instance, ProcessNode currentNode);

    void createConditionBranchTask(FlowInstance instance, ProcessNode currentNode);

    void createConditionNodeTask(FlowInstance instance, ProcessNodeCondition conditionNode);

}
