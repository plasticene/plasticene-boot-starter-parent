package com.plasticene.boot.flow.core.impl.validation.node;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.model.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.model.FlowFieldPermission;
import com.plasticene.boot.flow.core.model.FlowNode;
import com.plasticene.boot.flow.core.model.ProcessConditionGroup;
import com.plasticene.boot.flow.core.model.ProcessConditionRule;
import com.plasticene.boot.flow.core.model.ProcessNodeCondition;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证发布时在现有节点遍历中检查表单字段引用。
 * @author ZFJ
 * @since 2026-11-05
 */
class FlowFieldReferenceTest {
    @Test
    void shouldCheckPermissionsInChildAndBranchWithoutAnotherTreeTraversal() {
        var validator = validator();
        FlowNode root = node("root", 0);
        FlowNode branch = node("branch", 3);
        FlowNode approval = node("approval", 1);
        var permission = new FlowFieldPermission();
        permission.setField("subForm");
        approval.setFieldsPermission(List.of(permission));
        branch.setChildNode(approval);
        root.setConditionNodes(List.of(branch));
        assertTrue(assertThrows(BizException.class, () -> validator.validate(root, Set.of("amount"))).getMessage().contains("approval"));
        permission.setField("amount");
        assertDoesNotThrow(() -> validator.validate(root, Set.of("amount")));
    }

    @Test
    void shouldAllowBuiltinOnlyInConditionsAndRejectUnknownFields() {
        var validator = validator();
        FlowNode root = node("root", 1);
        var rule = new ProcessConditionRule();
        rule.setField("PROCESS_START_USER_ID");
        var group = new ProcessConditionGroup();
        group.setConditionRules(List.of(rule));
        var condition = new ProcessNodeCondition();
        condition.setConditionGroups(List.of(group));
        root.setCondition(condition);
        assertDoesNotThrow(() -> validator.validate(root, Set.of()));
        rule.setField("unknown");
        assertThrows(BizException.class, () -> validator.validate(root, Set.of()));
        rule.setField("amount");
        assertDoesNotThrow(() -> validator.validate(root, Set.of("amount")));
        var permission = new FlowFieldPermission();
        permission.setField("PROCESS_START_USER_ID");
        root.setFieldsPermission(List.of(permission));
        assertThrows(BizException.class, () -> validator.validate(root, Set.of("amount")));
    }

    private FlowNodeValidator validator() {
        var validator = new FlowNodeValidator();
        NodeValidator counter = new NodeValidator() {
            public boolean support(FlowNodeEnum.Type type) { return true; }
            public void validate(FlowNode node, ValidationContext context) {
                if (Integer.valueOf(1).equals(node.getType())) {
                    context.setApproveNodeCount(context.getApproveNodeCount() + 1);
                }
            }
        };
        ReflectionTestUtils.setField(validator, "validators", List.of(counter));
        return validator;
    }

    private FlowNode node(String key, int type) {
        var node = new FlowNode();
        node.setKey(key);
        node.setName(key);
        node.setType(type);
        return node;
    }
}
