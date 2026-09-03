package com.plasticene.boot.flow.core.model;

import java.util.List;

/**
 * 条件分支的两级条件组结构。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public record FlowCondition(FlowEnums.LogicalOperator groupOperator, List<Group> groups) {

    public FlowCondition {
        groupOperator = groupOperator == null ? FlowEnums.LogicalOperator.OR : groupOperator;
        groups = groups == null ? List.of() : List.copyOf(groups);
    }

    public record Group(FlowEnums.LogicalOperator ruleOperator, List<Rule> rules) {
        public Group {
            ruleOperator = ruleOperator == null ? FlowEnums.LogicalOperator.AND : ruleOperator;
            rules = rules == null ? List.of() : List.copyOf(rules);
        }
    }

    public record Rule(String variable, FlowEnums.ComparisonOperator operator, Object expectedValue) {
        public Rule {
            if (variable == null || variable.isBlank()) {
                throw new IllegalArgumentException("condition variable must not be blank");
            }
            if (operator == null) {
                throw new IllegalArgumentException("condition operator must not be null");
            }
        }
    }
}
