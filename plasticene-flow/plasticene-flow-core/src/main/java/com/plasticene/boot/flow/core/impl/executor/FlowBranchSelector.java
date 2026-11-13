package com.plasticene.boot.flow.core.impl.executor;

import cn.hutool.core.collection.CollUtil;
import com.plasticene.boot.flow.core.model.enums.FlowConditionTypeEnum;
import com.plasticene.boot.flow.core.impl.condition.OperatorFactory;
import com.plasticene.boot.flow.core.model.FlowNode;
import com.plasticene.boot.flow.core.model.ProcessConditionGroup;
import com.plasticene.boot.flow.core.model.ProcessConditionRule;
import com.plasticene.boot.flow.core.model.ProcessNodeCondition;
import com.plasticene.boot.flow.core.impl.condition.Operator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 根据流程变量选择条件分支与并行分支，不执行任务或修改节点。
 * @author ZFJ
 * @since 2026-11-10
 */
final class FlowBranchSelector {

    /** 按配置顺序选择首个匹配分支，无匹配时返回 null。 */
    public FlowNode selectExclusiveBranch(FlowNode gateway, Map<String, Object> varMap) {
        for (FlowNode branch : gateway.getConditionNodes()) {
            if (matches(varMap, branch)) {
                return branch;
            }
        }
        return null;
    }

    /** 选择全部匹配的普通分支，仅在无匹配时使用默认分支。 */
    public List<FlowNode> selectParallelBranches(FlowNode gateway, Map<String, Object> varMap) {
        List<FlowNode> matched = new ArrayList<>();
        FlowNode fallback = null;
        for (FlowNode branch : gateway.getConditionNodes()) {
            if (branch.getCondition() != null && Boolean.TRUE.equals(branch.getCondition().getIsDefault())) {
                fallback = branch;
            } else if (matches(varMap, branch)) {
                matched.add(branch);
            }
        }
        if (matched.isEmpty() && fallback != null) {
            matched.add(fallback);
        }
        return matched;
    }

    private boolean matches(Map<String, Object> varMap, FlowNode conditionNode) {
        // 条件节点的条件配置
        ProcessNodeCondition condition = conditionNode.getCondition();
        if (condition == null) {
            // 没有条件配置直接通过，默认条件节点就是没有条件的
            return true;
        }
        List<ProcessConditionGroup> conditionGroups = condition.getConditionGroups();
        if (CollUtil.isEmpty(conditionGroups)) {
            return true;
        }
        // 校验条件规则
        Integer matchType = condition.getType();
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
        if (Objects.isNull(varMap)) {
            return false;
        }
        for (ProcessConditionRule rule : conditionRules) {
            String field = rule.getField();
            Integer fieldType = rule.getType();
            Object fieldValue = varMap.get(field);
            String op = rule.getOperator();
            String inputValue = rule.getInputValue();
            Operator operator = OperatorFactory.getOperator(op);
            boolean compare = operator.compare(fieldType, fieldValue, inputValue);
            if (andMatch && !compare) {
                return false;
            }
            if (!andMatch && compare) {
                return true;
            }
        }
        return andMatch;
    }

}
