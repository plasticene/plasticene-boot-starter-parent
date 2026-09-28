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

        assertDoesNotThrow(() -> validator.validate(node, new ValidationContext()));
    }

    @Test
    void shouldRejectConditionNodeWithoutCondition() {
        FlowNode node = conditionNode("Condition_P5OG_gNfGu");

        assertThrows(BizException.class, () -> validator.validate(node, new ValidationContext()));
    }

    private FlowNode conditionNode(String key) {
        FlowNode node = new FlowNode();
        node.setKey(key);
        node.setType(FlowNodeEnum.Type.CONDITION_NODE.getCode());
        return node;
    }
}
