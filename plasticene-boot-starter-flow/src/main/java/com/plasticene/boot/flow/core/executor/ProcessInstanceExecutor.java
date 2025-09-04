package com.plasticene.boot.flow.core.executor;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.plasticene.boot.flow.core.dto.ProcessConditionGroup;
import com.plasticene.boot.flow.core.dto.ProcessConditionRule;
import com.plasticene.boot.flow.core.dto.ProcessNode;
import com.plasticene.boot.flow.core.dto.ProcessNodeCondition;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.enums.FlowConditionTypeEnum;
import com.plasticene.boot.flow.core.enums.FlowProcessNodeEnum;
import com.plasticene.boot.flow.core.factory.OperatorFactory;
import com.plasticene.boot.flow.core.parser.FlowParser;
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
    }


    private void handleStartNode(FlowInstance instance, ProcessNode currentNode) {
        flowTaskService.createStartTask(instance, currentNode);
        // 流入下一个节点
        moveToNextNode(instance, currentNode);
    }

    private void handleApproveNode(FlowInstance instance, ProcessNode currentNode) {
        flowTaskService.createApproveTask(instance, currentNode);
        Integer approveType = currentNode.getApproveType();
        if (Objects.equals(approveType, FlowProcessNodeEnum.ApproveType.AUTO_PASS.getCode())) {
            moveToNextNode(instance, currentNode);
        }
        if (Objects.equals(approveType, FlowProcessNodeEnum.ApproveType.AUTO_REJECT.getCode())) {
            // todo 完成流程实例，发布通知
        }
    }

    private void handleCopyNode(FlowInstance instance, ProcessNode currentNode) {
        flowTaskService.createCopyTask(instance, currentNode);
        moveToNextNode(instance, currentNode);
    }

    private void handleEndNode(FlowInstance instance, ProcessNode currentNode) {
        flowTaskService.createEndTask(instance, currentNode);
        // todo 完成流程实例，发布通知

    }

    public void handleConditionBranch(FlowInstance instance, ProcessNode currentNode) {
        flowTaskService.createConditionBranchTask(instance, currentNode);
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

    public boolean handleConditionNode(FlowInstance instance, ProcessNodeCondition conditionNode) {
        List<ProcessConditionGroup> conditionGroups = conditionNode.getConditionGroups();

        // 没有条件配置直接通过，默认条件节点就是没有条件的
        if (CollUtil.isEmpty(conditionGroups)) {
            return true;
        }
        // 校验条件规则
        Map<String, Object> varMap = JSON.parseObject(instance.getVarMap(), new TypeReference<Map<String, Object>>(){});
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


    private void moveToNextNode(FlowInstance instance, ProcessNode currentNode) {
        ProcessNode nextNode = FlowParser.findExecutionNextNode(currentNode);
        executeNode(instance, nextNode);
    }





}
