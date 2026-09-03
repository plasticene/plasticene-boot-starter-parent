package com.plasticene.boot.flow.engine;

import com.plasticene.boot.flow.core.FlowException;
import com.plasticene.boot.flow.core.model.FlowBranch;
import com.plasticene.boot.flow.core.model.FlowCondition;
import com.plasticene.boot.flow.core.model.FlowEnums;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 使用受控操作符计算流程条件分支。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public class FlowConditionEvaluator {

    public FlowBranch select(List<FlowBranch> branches, Map<String, Object> variables) {
        FlowBranch defaultBranch = null;
        for (FlowBranch branch : branches) {
            if (branch.defaultBranch()) {
                defaultBranch = branch;
            } else if (matches(branch.condition(), variables)) {
                return branch;
            }
        }
        if (defaultBranch != null) {
            return defaultBranch;
        }
        throw new FlowException("FLOW_CONDITION_NOT_MATCHED", "No condition branch matched");
    }

    boolean matches(FlowCondition condition, Map<String, Object> variables) {
        return combine(condition.groupOperator(), condition.groups().stream()
                .map(group -> combine(group.ruleOperator(), group.rules().stream()
                        .map(rule -> matches(rule, variables)).toList()))
                .toList());
    }

    private boolean matches(FlowCondition.Rule rule, Map<String, Object> variables) {
        Object actual = variables.get(rule.variable());
        return switch (rule.operator()) {
            case EQ -> equal(actual, rule.expectedValue());
            case NE -> !equal(actual, rule.expectedValue());
            case GT -> compare(actual, rule.expectedValue()) > 0;
            case GE -> compare(actual, rule.expectedValue()) >= 0;
            case LT -> compare(actual, rule.expectedValue()) < 0;
            case LE -> compare(actual, rule.expectedValue()) <= 0;
            case IN -> contains(rule.expectedValue(), actual);
            case NOT_IN -> !contains(rule.expectedValue(), actual);
            case CONTAINS -> contains(actual, rule.expectedValue());
            case NOT_EMPTY -> !isEmpty(actual);
            case EMPTY -> isEmpty(actual);
        };
    }

    private boolean combine(FlowEnums.LogicalOperator operator, List<Boolean> values) {
        if (values.isEmpty()) {
            return false;
        }
        return operator == FlowEnums.LogicalOperator.AND
                ? values.stream().allMatch(Boolean::booleanValue)
                : values.stream().anyMatch(Boolean::booleanValue);
    }

    private int compare(Object actual, Object expected) {
        if (actual == null || expected == null) {
            throw new FlowException("FLOW_CONDITION_VALUE_INVALID", "Ordered comparison does not accept null");
        }
        if (actual instanceof Number && expected instanceof Number) {
            return new BigDecimal(actual.toString()).compareTo(new BigDecimal(expected.toString()));
        }
        if (actual instanceof String actualText && expected instanceof String expectedText) {
            return actualText.compareTo(expectedText);
        }
        throw new FlowException("FLOW_CONDITION_VALUE_INVALID", "Values are not comparable");
    }

    private boolean contains(Object container, Object value) {
        if (container instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> equal(item, value));
        }
        if (container instanceof String text && value != null) {
            return text.contains(value.toString());
        }
        return false;
    }

    private boolean isEmpty(Object value) {
        return value == null
                || value instanceof String text && text.isBlank()
                || value instanceof Collection<?> collection && collection.isEmpty()
                || value instanceof Map<?, ?> map && map.isEmpty();
    }

    private Object normalize(Object value) {
        return value instanceof Number ? new BigDecimal(value.toString()) : value;
    }

    private boolean equal(Object first, Object second) {
        if (first instanceof Number && second instanceof Number) {
            return new BigDecimal(first.toString()).compareTo(new BigDecimal(second.toString())) == 0;
        }
        return Objects.equals(normalize(first), normalize(second));
    }
}
