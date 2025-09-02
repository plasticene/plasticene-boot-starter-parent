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
import com.plasticene.boot.flow.core.service.FlowTaskService;
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
        flowTaskService.createConditionTask(instance, currentNode);
        List<ProcessNodeCondition> conditionNodes = currentNode.getConditionNodes();
//        Boolean match = handleConditionNode(instance, conditionNodes);
        moveToNextNode(instance, currentNode);
    }

    public Boolean handleConditionNode(FlowInstance instance, ProcessNodeCondition conditionNode) {
        ProcessNode childNode = conditionNode.getChildNode();
        List<ProcessConditionGroup> conditionGroups = conditionNode.getConditionGroups();
        // 没有条件配置并且没有子节点，直接回退到流程主干分支执行下一个节点
        if (CollUtil.isEmpty(conditionGroups) && Objects.isNull(childNode)) {
            return true;
        }
        // 没有条件配置，但有子节点，直接流入下一个节点
        if (CollUtil.isEmpty(conditionGroups) && Objects.nonNull(childNode)) {
            executeNode(instance, childNode);
        }

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
            String attr = rule.getAttr();
            Object value = varMap.get(attr);
            String operator = rule.getOperator();
            String inputValue = rule.getInputValue();
//            OperComparator comparator = ComparatorFactory.getComparator(operator);
//            boolean compare = comparator.compare(value, inputValue);
            boolean compare = true;
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
        ProcessNode childNode = currentNode.getChildNode();
        executeNode(instance, childNode);
    }





}
