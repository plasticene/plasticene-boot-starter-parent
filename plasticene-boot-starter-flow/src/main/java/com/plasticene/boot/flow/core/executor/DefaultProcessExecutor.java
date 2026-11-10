package com.plasticene.boot.flow.core.executor;

import com.plasticene.boot.flow.core.enums.FlowApproveTaskResult;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import jakarta.annotation.Resource;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <b>流程执行器</b>: 节点流转中枢
 * @author ZFJ
 * @date 2025/9/2
 */
public class DefaultProcessExecutor implements ProcessExecutor {
    @Resource
    private FlowTaskService flowTaskService;
    @Resource
    private FlowRuntimeService flowRuntimeService;

    private final FlowBranchSelector branchSelector = new FlowBranchSelector();
    private final FlowRouteCalculator routeCalculator = new FlowRouteCalculator(branchSelector);

    @Override
    public void executeNode(FlowInstance instance, FlowNode currentNode) {
        Integer type = currentNode.getType();
        if (Objects.equals(type, FlowNodeEnum.Type.START.getCode())) {
            handleStartNode(instance, currentNode);
        }
        if (Objects.equals(type, FlowNodeEnum.Type.END.getCode())) {
            handleEndNode(instance, currentNode);
        }
        if (Objects.equals(type, FlowNodeEnum.Type.APPROVE.getCode())) {
            handleApproveNode(instance, currentNode);
        }
        if (Objects.equals(type, FlowNodeEnum.Type.COPY.getCode())) {
            handleCopyNode(instance, currentNode);
        }
        if (Objects.equals(type, FlowNodeEnum.Type.CONDITION_BRANCH.getCode())) {
            handleConditionBranch(instance, currentNode);
        }
        if (Objects.equals(type, FlowNodeEnum.Type.PARALLEL_BRANCH.getCode())) {
            handleParallelBranch(instance, currentNode);
        }
    }

    @Override
    public void moveToNextNode(FlowInstance instance, FlowNode currentNode) {
        FlowNode completedNode = currentNode;
        while (completedNode != null) {
            if (completedNode.getChildNode() != null) {
                executeNode(instance, completedNode.getChildNode());
                return;
            }
            // 沿当前分支向上查找出口，逐层完成嵌套网关的汇合。
            FlowNode branch = completedNode;
            while (branch != null && !Objects.equals(branch.getType(), FlowNodeEnum.Type.CONDITION_NODE.getCode())) {
                branch = branch.getParentNode();
            }
            if (branch == null) {
                return;
            }
            FlowNode gateway = branch.getParentNode();
            if (Objects.equals(gateway.getType(), FlowNodeEnum.Type.PARALLEL_BRANCH.getCode())) {
                int completed = flowTaskService.incrementCompletedBranchAndGet(instance.getId(), gateway.getKey());
                if (completed != gateway.getConditionNodes().size()) {
                    return;
                }
            }
            completedNode = gateway;
        }
    }

    @Override
    public List<FlowNode> calculateRoute(FlowNode flowNode, Map<String, Object> varMap) {
        return routeCalculator.calculateRoute(flowNode, varMap);
    }

    @Override
    public List<FlowNode> calculateRouteTrace(FlowNode flowNode, Map<String, Object> varMap) {
        return routeCalculator.calculateRouteTrace(flowNode, varMap);
    }

    /**
     * 处理开始节点
     */
    private void handleStartNode(FlowInstance instance, FlowNode currentNode) {
        flowTaskService.createStartTask(instance, currentNode);
        // 流入下一个节点
        moveToNextNode(instance, currentNode);
    }

    /**
     * 处理审批节点
     */
    private void handleApproveNode(FlowInstance instance, FlowNode currentNode) {
        flowRuntimeService.updateInstanceCurrentNode(instance.getId(), currentNode);
        FlowApproveTaskResult result = flowTaskService.createApproveTask(instance, currentNode);
        switch (result) {
            case WAITING -> {}
            case APPROVED -> moveToNextNode(instance, currentNode);
            case REJECTED -> flowRuntimeService.endInstance(instance, currentNode, FlowInstanceStatusEnum.REJECT);
        }
    }

    private void handleCopyNode(FlowInstance instance, FlowNode currentNode) {
        flowTaskService.createCopyTask(instance, currentNode);
        moveToNextNode(instance, currentNode);
    }

    private void handleEndNode(FlowInstance instance, FlowNode currentNode) {
        flowTaskService.createEndTask(instance, currentNode);
        flowRuntimeService.endInstance(instance, currentNode, FlowInstanceStatusEnum.APPROVE);
    }

    private void handleConditionBranch(FlowInstance instance, FlowNode currentNode) {
        flowTaskService.createBranchTask(instance, currentNode);
        FlowNode conditionNode = branchSelector.selectExclusiveBranch(currentNode, instance.getVarMap());
        if (conditionNode == null) {
            return;
        }
        flowTaskService.createConditionNodeTask(instance, conditionNode);
        FlowNode childNode = conditionNode.getChildNode();
        if (childNode != null) {
            executeNode(instance, childNode);
        } else {
            moveToNextNode(instance, conditionNode);
        }
    }

    private void handleParallelBranch(FlowInstance instance, FlowNode currentNode) {
        List<FlowNode> conditionNodes = branchSelector.selectParallelBranches(currentNode, instance.getVarMap());
        flowTaskService.createBranchTask(instance, currentNode);
        // 先持久化跳过数量，避免同步完成的空分支或自动审批提前汇合。
        int skipped = currentNode.getConditionNodes().size() - conditionNodes.size();
        for (int i = 0; i < skipped; i++) {
            flowTaskService.incrementCompletedBranchAndGet(instance.getId(), currentNode.getKey());
        }
        for (FlowNode conditionNode : conditionNodes) {
            Integer status = instance.getStatus();
            if (!Objects.equals(status, FlowInstanceStatusEnum.RUNNING.getCode())) {
                // 实例状态不是运行中，则结束   eg:第一个分支自动拒绝了，后续分支就没必要执行了
                break;
            }
            flowTaskService.createConditionNodeTask(instance, conditionNode);
            // 条件节点是否有子节点
            FlowNode childNode = conditionNode.getChildNode();
            if (childNode != null) {
                // 有，流转到条件节点的子节点
                executeNode(instance, childNode);
            } else {
                // 没有子节点，那就回退到当前条件分支，执行条件分支的其他子节点
                moveToNextNode(instance, conditionNode);
            }
        }

    }



}
