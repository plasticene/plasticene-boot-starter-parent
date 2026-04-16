package com.plasticene.boot.flow.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowRuntimeService extends IService<FlowInstance> {

    /**
     * 根据流程模型id发起一次流程实例
     * @param processId 流程模型id
     * @return 实例id
     */
    @Transactional(rollbackFor = Exception.class)
    default long startFlowInstanceById(Long processId) {
        return startFlowInstanceById(processId, null, null);
    }

    /**
     * 根据流程模型id发起一次流程实例
     * @param processId 流程模型id
     * @param businessId 申请业务id
     * @return 实例id
     */
    @Transactional(rollbackFor = Exception.class)
    default long startFlowInstanceById(Long processId, Long businessId) {
        return startFlowInstanceById(processId, businessId, null);
    }

    /**
     * 根据流程模型id发起一次流程实例
     * @param processId 流程模型id
     * @param businessId  申请业务id
     * @param varMap 变量map
     * @return 实例id
     */
    long startFlowInstanceById(Long processId, Long businessId, Map<String, Object> varMap);

    /**
     * 对实例加行锁，保证并发情况下数据准确性
     * 一般情况不会触发，但是极限同时审批时可能出现下面情况：
     * 1、或签：两个人极限情况下同时审批时，应该以第一个人的审批为准，但是可能两个人都审批了导致错误
     * 2、会签：两个人极限情况下同时审批时，判断是否都审批过无法准确判断，导致流程没法正常流转
     * @param instanceId 实例id
     * @return 实例信息
     */
    FlowInstance selectInstanceForUpdate(Long instanceId);

    /**
     * 更新流程实例执行到的当前节点
     * @param instanceId 实例id
     * @param currentNode 当前节点
     */
    void updateInstanceCurrentNode(Long instanceId, FlowNode currentNode);

    /**
     * 结束流程实例
     * @param instance 实例
     * @param currentNode 当前节点
     * @param status 实例状态
     */
    void endInstance(FlowInstance instance, FlowNode currentNode, FlowInstanceStatusEnum status);

}
