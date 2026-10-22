package com.plasticene.boot.flow.core.validator;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 条件节点校验测试
 *
 * @author ZFJ
 * @since 2026-09-23
 */
class ConditionNodeValidatorTest {

    private final ConditionNodeValidator validator = new ConditionNodeValidator();

    @Test
    void shouldAllowParallelNodeWithoutCondition() {
        FlowNode node = conditionNode("Parallel_P5OG_gNfGu");

        ValidationContext context = new ValidationContext();
        context.setParentNodeType(FlowNodeEnum.Type.PARALLEL_BRANCH.getCode());
        assertDoesNotThrow(() -> validator.validate(node, context));
    }

    @Test
    void shouldRejectConditionNodeWithoutCondition() {
        FlowNode node = conditionNode("Condition_P5OG_gNfGu");

        assertThrows(BizException.class, () -> validator.validate(node, new ValidationContext()));
    }

    @Test
    void shouldNotTrustParallelKeyPrefixOutsideParallelGateway() {
        assertThrows(BizException.class, () -> validator.validate(
                conditionNode("Parallel_fake"), new ValidationContext()));
    }

    @Test
    void shouldValidateConfiguredParallelConditions() {
        FlowNode node = conditionNode("custom-key");
        node.setCondition(new com.plasticene.boot.flow.core.model.dto.ProcessNodeCondition());
        ValidationContext context = new ValidationContext();
        context.setParentNodeType(FlowNodeEnum.Type.PARALLEL_BRANCH.getCode());
        assertThrows(BizException.class, () -> validator.validate(node, context));
    }

    @Test
    void shouldRequireUniqueFallbackAtEnd() {
        FlowNodeValidator treeValidator = new FlowNodeValidator();
        org.springframework.test.util.ReflectionTestUtils.setField(treeValidator, "validators",
                java.util.List.of(validator, new ApproveNodeValidator()));
        FlowNode gateway = new FlowNode();
        gateway.setKey("gateway");
        gateway.setType(FlowNodeEnum.Type.PARALLEL_BRANCH.getCode());
        FlowNode approve = new FlowNode();
        approve.setKey("approve");
        approve.setType(FlowNodeEnum.Type.APPROVE.getCode());
        approve.setApproveType(0);
        approve.setAssigneeType(0);
        gateway.setChildNode(approve);
        FlowNode ordinary = conditionNode("ordinary");
        FlowNode fallback = conditionNode("fallback");
        var condition = new com.plasticene.boot.flow.core.model.dto.ProcessNodeCondition();
        condition.setIsDefault(true);
        fallback.setCondition(condition);
        gateway.setConditionNodes(java.util.List.of(ordinary, fallback));
        assertDoesNotThrow(() -> treeValidator.validate(gateway));
        gateway.setConditionNodes(java.util.List.of(ordinary));
        assertThrows(BizException.class, () -> treeValidator.validate(gateway));
        gateway.setConditionNodes(java.util.List.of(fallback, ordinary));
        assertThrows(BizException.class, () -> treeValidator.validate(gateway));
        FlowNode duplicate = conditionNode("duplicate");
        duplicate.setCondition(condition);
        gateway.setConditionNodes(java.util.List.of(ordinary, duplicate, fallback));
        assertThrows(BizException.class, () -> treeValidator.validate(gateway));
        gateway.setConditionNodes(java.util.List.of(ordinary, fallback));
        condition.setType(0);
        assertThrows(BizException.class, () -> treeValidator.validate(gateway));
    }

    private FlowNode conditionNode(String key) {
        FlowNode node = new FlowNode();
        node.setKey(key);
        node.setType(FlowNodeEnum.Type.CONDITION_NODE.getCode());
        return node;
    }
}
