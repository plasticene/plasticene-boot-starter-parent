package com.plasticene.boot.flow.core.validator;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.dto.FlowTimeoutRule;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证审批节点自定义超时规则及流程树中的校验。
 * @author ZFJ
 * @since 2026-10-29
 */
class ApproveNodeValidatorTest {

    private final ApproveNodeValidator validator = new ApproveNodeValidator();

    @Test
    void shouldRequireValidCustomTimeoutRule() {
        FlowNode node = approveNode("approve");
        node.setTimeoutMode(FlowTimeoutRule.Mode.CUSTOM);
        assertThrows(BizException.class, () -> validator.validate(node, new ValidationContext()));

        FlowTimeoutRule rule = new FlowTimeoutRule();
        node.setTimeoutRule(rule);
        rule.setAutoEnabled(true);
        assertThrows(BizException.class, () -> validator.validate(node, new ValidationContext()));
        rule.setAutoAfter(2);
        rule.setAction(FlowTimeoutRule.Action.APPROVE);
        assertDoesNotThrow(() -> validator.validate(node, new ValidationContext()));
        rule.setAutoEnabled(false);
        assertDoesNotThrow(() -> validator.validate(node, new ValidationContext()));
    }

    @Test
    void shouldSkipUnusedRulesForInheritedAndDisabledModes() {
        FlowNode node = approveNode("approve");
        FlowTimeoutRule invalidRule = new FlowTimeoutRule();
        invalidRule.setReminderEnabled(true);
        node.setTimeoutRule(invalidRule);
        assertDoesNotThrow(() -> validator.validate(node, new ValidationContext()));
        node.setTimeoutMode(FlowTimeoutRule.Mode.INHERIT);
        assertDoesNotThrow(() -> validator.validate(node, new ValidationContext()));
        node.setTimeoutMode(FlowTimeoutRule.Mode.DISABLED);
        assertDoesNotThrow(() -> validator.validate(node, new ValidationContext()));
    }

    @Test
    void shouldValidateCustomTimeoutInChildAndConditionBranches() {
        FlowNodeValidator treeValidator = new FlowNodeValidator();
        ReflectionTestUtils.setField(treeValidator, "validators", List.of(validator));
        FlowNode root = approveNode("root");
        FlowNode child = approveNode("child");
        FlowNode branch = new FlowNode();
        branch.setKey("branch");
        branch.setType(FlowNodeEnum.Type.CONDITION_NODE.getCode());
        FlowNode nested = approveNode("nested");
        root.setChildNode(child);
        root.setConditionNodes(List.of(branch));
        branch.setChildNode(nested);

        child.setTimeoutMode(FlowTimeoutRule.Mode.CUSTOM);
        assertThrows(BizException.class, () -> treeValidator.validate(root));
        child.setTimeoutRule(new FlowTimeoutRule());
        nested.setTimeoutMode(FlowTimeoutRule.Mode.CUSTOM);
        assertThrows(BizException.class, () -> treeValidator.validate(root));
        nested.setTimeoutRule(new FlowTimeoutRule());
        assertDoesNotThrow(() -> treeValidator.validate(root));
    }

    private FlowNode approveNode(String key) {
        FlowNode node = new FlowNode();
        node.setKey(key);
        node.setType(FlowNodeEnum.Type.APPROVE.getCode());
        node.setApproveType(0);
        node.setAssigneeType(0);
        return node;
    }
}
