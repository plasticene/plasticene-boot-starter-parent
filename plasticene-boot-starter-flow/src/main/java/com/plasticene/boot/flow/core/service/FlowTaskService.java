package com.plasticene.boot.flow.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.model.dto.ProcessNode;
import com.plasticene.boot.flow.core.model.dto.ProcessNodeCondition;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;

import java.util.List;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowTaskService extends IService<FlowTask> {

    /**
     * 创建开始节点任务
     */
    void createStartTask(FlowInstance instance, ProcessNode currentNode);

    /**
     * 创建审批节点任务
     */
    void createApproveTask(FlowInstance instance, ProcessNode currentNode);

    /**
     * 创建抄送节点任务
     */
    void createCopyTask(FlowInstance instance, ProcessNode currentNode);

    /**
     * 创建结束节点任务
     */
    void createEndTask(FlowInstance instance, ProcessNode currentNode);

    /**
     * 创建分支任务
     */
    void createBranchTask(FlowInstance instance, ProcessNode currentNode);

    /**
     * 创建条件节点任务
     */
    void createConditionNodeTask(FlowInstance instance, ProcessNodeCondition conditionNode);

    /**
     * 审批任务
     */
    void approveTask(FlowTaskParam param);

    /**
     * 拒绝任务
     */
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

    /**
     * 获取流程实例下所有任务.
     * @param instanceId 实例id
     * @return 任务列表
     */
    List<FlowTask> listTaskByInstanceId(Long instanceId);


}
