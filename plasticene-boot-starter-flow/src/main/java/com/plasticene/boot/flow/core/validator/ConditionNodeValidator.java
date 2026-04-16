package com.plasticene.boot.flow.core.validator;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.factory.OperatorFactory;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.dto.ProcessConditionGroup;
import com.plasticene.boot.flow.core.model.dto.ProcessConditionRule;
import com.plasticene.boot.flow.core.model.dto.ProcessNodeCondition;
import com.plasticene.boot.flow.core.operator.Operator;

import java.util.List;

/**
 * @author ZFJ
 * @since 2026/4/15
 */
public class ConditionNodeValidator implements NodeValidator{
    @Override
    public boolean support(FlowNodeEnum.Type nodeType) {
        return FlowNodeEnum.Type.CONDITION_NODE.equals(nodeType);
    }

    @Override
    public void validate(FlowNode node, ValidationContext context) {
        ProcessNodeCondition condition = node.getCondition();
        if (condition == null) {
            throw new BizException("条件节点: {0}, 条件配置不能为空", node.getKey());
        }
        if (Boolean.TRUE.equals(condition.getIsDefault())) {
            return;
        }
        if (CollUtil.isEmpty(condition.getConditionGroups())) {
            throw new BizException("条件节点: {0}, 条件组不能为空", node.getKey());
        }
        condition.getConditionGroups().forEach(group -> {
            List<ProcessConditionRule> conditionRules = group.getConditionRules();
            if (CollUtil.isEmpty(conditionRules)) {
                throw new BizException("条件节点: {0}, 条件组规则不能为空", node.getKey());
            }
            conditionRules.forEach(rule -> {
                String field = rule.getField();
                if (StrUtil.isBlank(field)) {
                    throw new BizException("条件节点: {0}, 节点条件组规则字段不能为空", node.getKey());
                }
                String op = rule.getOperator();
                if (StrUtil.isBlank(op)) {
                    throw new BizException("条件节点: {0}, 条件组规则: {1}, 运算符不能为空", node.getKey(), rule.getField());
                }
                Operator operator = OperatorFactory.getOperator(op);
                if (operator == null) {
                    throw new BizException("条件节点: {0}, 节点条件组规则: {1}, 运算符: {2}, 不支持", node.getKey(), op, op);
                }
                // 校验输入值
                operator.validate(rule.getType(), rule.getInputValue());

            });
        });

    }
}
