package com.plasticene.boot.flow.core.service;

import com.plasticene.boot.flow.core.model.dto.ProcessNode;
import com.plasticene.boot.flow.core.model.dto.ProcessNodeCondition;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowTaskService {

    void createStartTask(FlowInstance instance, ProcessNode currentNode);

    void createApproveTask(FlowInstance instance, ProcessNode currentNode);

    void createCopyTask(FlowInstance instance, ProcessNode currentNode);

    void createEndTask(FlowInstance instance, ProcessNode currentNode);

    void createBranchTask(FlowInstance instance, ProcessNode currentNode);

    void createConditionNodeTask(FlowInstance instance, ProcessNodeCondition conditionNode);

    void approveTask(FlowTaskParam param);

    void rejectTask(FlowTaskParam param);

    /**
     * 并行分支任务完成的分支数量+1
     * @param instanceId 实例id
     * @param nodeKey 并行分支节点key
     * @return 最新完成分支数量
     */
    int incrementCompletedBranchAndGet(Long instanceId, String nodeKey);

    /**
     * 删除流程实例下其他正在执行中的任务.
     * @param instanceId 实例id
     */
    void delInstanceRunningTask(Long instanceId);


}
