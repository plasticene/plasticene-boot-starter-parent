package com.plasticene.boot.flow.core.executor;

import cn.hutool.core.collection.CollUtil;
import com.plasticene.boot.flow.core.model.dto.ProcessConditionGroup;
import com.plasticene.boot.flow.core.model.dto.ProcessConditionRule;
import com.plasticene.boot.flow.core.model.dto.ProcessNode;
import com.plasticene.boot.flow.core.model.dto.ProcessNodeCondition;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.enums.FlowConditionTypeEnum;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowProcessNodeEnum;
import com.plasticene.boot.flow.core.factory.OperatorFactory;
import com.plasticene.boot.flow.core.parser.FlowParser;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import com.plasticene.boot.flow.core.operator.Operator;
import jakarta.annotation.Resource;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public class ProcessInstanceExecutor {
    @Resource
    private FlowTaskService flowTaskService;
    @Resource
    private FlowRuntimeService flowRuntimeService;

    public void executeNode(FlowInstance instance, ProcessNode currentNode) {
        Integer type = currentNode.getType();
        if (Objects.equals(type, FlowProcessNodeEnum.Type.START.getCode())) {
            handleStartNode(instance, currentNode);
        }
        if (Objects.equals(type, FlowProcessNodeEnum.Type.END.getCode())) {
            handleEndNode(instance, currentNode);
        }
        if (Objects.equals(type, FlowProcessNodeEnum.Type.APPROVE.getCode())) {
            handleApproveNode(instance, currentNode);
        }
        if (Objects.equals(type, FlowProcessNodeEnum.Type.COPY.getCode())) {
            handleCopyNode(instance, currentNode);
        }
        if (Objects.equals(type, FlowProcessNodeEnum.Type.CONDITION_BRANCH.getCode())) {
            handleConditionBranch(instance, currentNode);
        }
        if (Objects.equals(type, FlowProcessNodeEnum.Type.PARALLEL_BRANCH.getCode())) {
            handleParallelBranch(instance, currentNode);
        }
    }

    public void moveToNextNode(FlowInstance instance, ProcessNode currentNode) {
        ProcessNode nextNode = FlowParser.findExecutionNextNode(currentNode);
        ProcessNode parentNode = nextNode.getParentNode();
        // 如果要执行的下一个节点的父节点是并行分支，需要判断所有分支是否已经执行，才能流入下一个节点
        if (Objects.equals(parentNode.getType(), FlowProcessNodeEnum.Type.PARALLEL_BRANCH.getCode())) {
            // 并行分支完成分支数+1，并返回最新完成分支数
            int completedBranch = flowTaskService.incrementCompletedBranchAndGet(instance.getId(), parentNode.getKey());
            // 并行分支数
            int branches = parentNode.getConditionNodes().size();
            if (completedBranch != branches) {
                // 完成的分支数不等于并行分支数，不流入下一个节点
                return;
            }
        }
        executeNode(instance, nextNode);
    }


    /**
     * 处理开始节点
     */
    private void handleStartNode(FlowInstance instance, ProcessNode currentNode) {
        flowTaskService.createStartTask(instance, currentNode);
        // 流入下一个节点
        moveToNextNode(instance, currentNode);
    }

    /**
     * 处理审批节点
     */
    private void handleApproveNode(FlowInstance instance, ProcessNode currentNode) {
        flowTaskService.createApproveTask(instance, currentNode);
        flowRuntimeService.updateInstanceCurrentNode(instance.getId(), currentNode);
        Integer approveType = currentNode.getApproveType();
        // 自动通过 → 流入下一个节点
        if (Objects.equals(approveType, FlowProcessNodeEnum.ApproveType.AUTO_PASS.getCode())) {
            moveToNextNode(instance, currentNode);
        }
        // 自动拒绝 → 结束流程
        if (Objects.equals(approveType, FlowProcessNodeEnum.ApproveType.AUTO_REJECT.getCode())) {
            flowRuntimeService.endInstance(instance, currentNode, FlowInstanceStatusEnum.REJECT);
        }
    }

    private void handleCopyNode(FlowInstance instance, ProcessNode currentNode) {
        flowTaskService.createCopyTask(instance, currentNode);
        moveToNextNode(instance, currentNode);
    }

    private void handleEndNode(FlowInstance instance, ProcessNode currentNode) {
        flowTaskService.createEndTask(instance, currentNode);
        flowRuntimeService.updateInstanceCurrentNode(instance.getId(), currentNode);
        flowRuntimeService.endInstance(instance, currentNode, FlowInstanceStatusEnum.APPROVE);
    }

    private void handleConditionBranch(FlowInstance instance, ProcessNode currentNode) {
        flowTaskService.createBranchTask(instance, currentNode);
        List<ProcessNodeCondition> conditionNodes = currentNode.getConditionNodes();
        for (ProcessNodeCondition conditionNode : conditionNodes) {
            boolean match = handleConditionNode(instance, conditionNode);
            // 条件节点匹配成功
            if (match) {
                flowTaskService.createConditionNodeTask(instance, conditionNode);
                // 条件节点是否有子节点
                ProcessNode childNode = conditionNode.getChildNode();
                if (childNode != null) {
                    // 有，流转到条件节点的子节点
                    executeNode(instance, childNode);
                } else {
                    // 没有子节点，那就回退到当前条件分支，执行条件分支节点的子节点
                    moveToNextNode(instance, currentNode);
                }
                // 匹配成功后，同一个条件分支节点下的后续条件节点不再遍历
                break;
            }
        }
    }

    private boolean handleConditionNode(FlowInstance instance, ProcessNodeCondition conditionNode) {
        List<ProcessConditionGroup> conditionGroups = conditionNode.getConditionGroups();
        // 没有条件配置直接通过，默认条件节点就是没有条件的
        if (CollUtil.isEmpty(conditionGroups)) {
            return true;
        }
        // 校验条件规则
        Map<String, Object> varMap = instance.getVarMap();
        Integer matchType = conditionNode.getType();
        boolean andMatch = Objects.equals(matchType, FlowConditionTypeEnum.AND.getCode());
        for (ProcessConditionGroup conditionGroup : conditionGroups) {
            Boolean result = matchConditionGroup(varMap, conditionGroup);
            if (andMatch && !result) {
                return false;
            }
            if (!andMatch && result) {
                return true;
            }
        }
        return andMatch;
    }

    private Boolean matchConditionGroup(Map<String, Object> varMap, ProcessConditionGroup group) {
        Integer type = group.getType();
        boolean andMatch = Objects.equals(type, FlowConditionTypeEnum.AND.getCode());
        List<ProcessConditionRule> conditionRules = group.getConditionRules();
        if (CollUtil.isEmpty(conditionRules)) {
            return true;
        }
        for (ProcessConditionRule rule : conditionRules) {
            String field = rule.getField();
            Integer filedType = rule.getType();
            Object fieldValue = varMap.get(field);
            String op = rule.getOperator();
            String inputValue = rule.getInputValue();
            Operator operator = OperatorFactory.getOperator(op);
            boolean compare = operator.compare(filedType, fieldValue, inputValue);
            if (andMatch && !compare) {
                return false;
            }
            if (!andMatch && compare) {
                return true;
            }
        }
        return andMatch;
    }

    private void handleParallelBranch(FlowInstance instance, ProcessNode currentNode) {
        flowTaskService.createBranchTask(instance, currentNode);
        // 并行分支下的条件节点是不配置条件的，直接进入条件节点的下一个节点
        List<ProcessNodeCondition> conditionNodes = currentNode.getConditionNodes();
        for (ProcessNodeCondition conditionNode : conditionNodes) {
            // 条件节点是否有子节点
            ProcessNode childNode = conditionNode.getChildNode();
            if (childNode != null) {
                // 有，流转到条件节点的子节点
                executeNode(instance, childNode);
            } else {
                // 没有子节点，那就回退到当前条件分支，执行条件分支节点的子节点
                moveToNextNode(instance, currentNode);
            }
        }

    }



}
