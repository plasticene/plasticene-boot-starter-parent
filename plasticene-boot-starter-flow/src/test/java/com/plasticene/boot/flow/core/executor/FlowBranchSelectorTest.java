package com.plasticene.boot.flow.core.executor;

import com.plasticene.boot.flow.core.enums.FlowConditionTypeEnum;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.dto.ProcessConditionGroup;
import com.plasticene.boot.flow.core.model.dto.ProcessConditionRule;
import com.plasticene.boot.flow.core.model.dto.ProcessNodeCondition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证分支选择顺序、默认分支与条件组计算语义。
 * @author ZFJ
 * @since 2026-11-10
 */
class FlowBranchSelectorTest {
    private final FlowBranchSelector selector = new FlowBranchSelector();

    @Test
    void shouldSelectFirstMatchAndKeepExclusiveDefaultInConfiguredOrder() {
        FlowNode first = branch();
        first.getCondition().setIsDefault(true);
        FlowNode second = branch();
        FlowNode gateway = gateway(first, second);
        assertSame(first, selector.selectExclusiveBranch(gateway, Map.of()));
        assertEquals(List.of(second), selector.selectParallelBranches(gateway, Map.of()));
    }

    @Test
    void shouldMatchMissingConditionAndEmptyGroupsEvenWithoutVariables() {
        FlowNode absent = new FlowNode();
        FlowNode empty = branch();
        empty.getCondition().setConditionGroups(List.of());
        FlowNode gateway = gateway(absent, empty);
        assertSame(absent, selector.selectExclusiveBranch(gateway, null));
        assertEquals(List.of(absent, empty), selector.selectParallelBranches(gateway, null));
    }

    @Test
    void shouldMatchEmptyRuleGroupEvenWithoutVariables() {
        ProcessConditionGroup group = group();
        group.setConditionRules(List.of());
        FlowNode empty = branch(group);
        assertSame(empty, selector.selectExclusiveBranch(gateway(empty), null));
    }

    @ParameterizedTest
    @CsvSource({"0,yes,yes,true", "0,yes,no,false", "0,no,yes,false", "0,no,no,false",
            "1,yes,yes,true", "1,yes,no,true", "1,no,yes,true", "1,no,no,false"})
    void shouldApplyRuleRelations(int relation, String a, String b, boolean matches) {
        ProcessConditionGroup group = group(rule("a"), rule("b"));
        group.setType(relation);
        FlowNode branch = branch(group);
        assertEquals(matches, selector.selectExclusiveBranch(gateway(branch), Map.of("a", a, "b", b)) != null);
    }

    @ParameterizedTest
    @CsvSource({"0,yes,yes,true", "0,yes,no,false", "0,no,yes,false", "0,no,no,false",
            "1,yes,yes,true", "1,yes,no,true", "1,no,yes,true", "1,no,no,false"})
    void shouldApplyGroupRelations(int relation, String a, String b, boolean matches) {
        FlowNode branch = branch(group(rule("a")), group(rule("b")));
        branch.getCondition().setType(relation);
        assertEquals(matches, selector.selectExclusiveBranch(gateway(branch), Map.of("a", a, "b", b)) != null);
    }

    @Test
    void shouldNotMatchRulesWithoutVariablesOrRequiredField() {
        FlowNode gateway = gateway(branch(group(rule("a"))));
        assertNull(selector.selectExclusiveBranch(gateway, null));
        assertNull(selector.selectExclusiveBranch(gateway, Map.of()));
        assertEquals(List.of(), selector.selectParallelBranches(gateway, null));
    }

    @Test
    void shouldSelectAllMatchesInOrderAndUseDefaultOnlyWhenNoneMatch() {
        FlowNode first = branch(group(rule("a")));
        FlowNode second = branch(group(rule("b")));
        FlowNode fallback = branch();
        fallback.getCondition().setIsDefault(true);
        FlowNode gateway = gateway(first, fallback, second);
        assertEquals(List.of(first, second), selector.selectParallelBranches(gateway, Map.of("a", "yes", "b", "yes")));
        assertEquals(List.of(fallback), selector.selectParallelBranches(gateway, Map.of("a", "no", "b", "no")));
        assertEquals(List.of(), selector.selectParallelBranches(gateway(first, second), Map.of()));
    }

    @Test
    void shouldShortCircuitBeforeEvaluatingLaterInvalidRulesOrGroups() {
        ProcessConditionRule invalid = rule("invalid");
        invalid.setOperator("unsupported");
        ProcessConditionGroup andGroup = group(rule("a"), invalid);
        FlowNode andBranch = branch(andGroup);
        assertNull(selector.selectExclusiveBranch(gateway(andBranch), Map.of("a", "no")));
        ProcessConditionGroup orGroup = group(rule("a"), invalid);
        orGroup.setType(FlowConditionTypeEnum.OR.getCode());
        FlowNode orBranch = branch(orGroup);
        assertSame(orBranch, selector.selectExclusiveBranch(gateway(orBranch), Map.of("a", "yes")));
        FlowNode groups = branch(group(rule("a")), group(invalid));
        assertNull(selector.selectExclusiveBranch(gateway(groups), Map.of("a", "no")));
        groups.getCondition().setType(FlowConditionTypeEnum.OR.getCode());
        assertSame(groups, selector.selectExclusiveBranch(gateway(groups), Map.of("a", "yes")));
    }

    private FlowNode gateway(FlowNode... branches) {
        FlowNode node = new FlowNode();
        node.setConditionNodes(List.of(branches));
        return node;
    }

    private FlowNode branch(ProcessConditionGroup... groups) {
        FlowNode node = new FlowNode();
        node.setType(FlowNodeEnum.Type.CONDITION_NODE.getCode());
        ProcessNodeCondition condition = new ProcessNodeCondition();
        condition.setType(FlowConditionTypeEnum.AND.getCode());
        condition.setConditionGroups(List.of(groups));
        node.setCondition(condition);
        return node;
    }

    private ProcessConditionGroup group(ProcessConditionRule... rules) {
        ProcessConditionGroup group = new ProcessConditionGroup();
        group.setType(FlowConditionTypeEnum.AND.getCode());
        group.setConditionRules(List.of(rules));
        return group;
    }

    private ProcessConditionRule rule(String field) {
        ProcessConditionRule rule = new ProcessConditionRule();
        rule.setField(field);
        rule.setType(0);
        rule.setOperator("=");
        rule.setInputValue("yes");
        return rule;
    }
}
