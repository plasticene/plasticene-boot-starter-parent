package com.plasticene.boot.flow.core.impl.validation;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
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
        assertTrue(assertThrows(BizException.class, () -> validator.validate(root, schema())).getMessage().contains("approval"));
        permission.setField("amount");
        assertDoesNotThrow(() -> validator.validate(root, schema()));
    }

    @Test
    void shouldAllowBuiltinOnlyInConditionsAndRejectUnknownFields() {
        var validator = validator();
        FlowNode root = node("root", 1);
        var rule = new ProcessConditionRule();
        rule.setField("PROCESS_START_USER_ID");
        rule.setType(1);
        var group = new ProcessConditionGroup();
        group.setConditionRules(List.of(rule));
        var condition = new ProcessNodeCondition();
        condition.setConditionGroups(List.of(group));
        root.setCondition(condition);
        assertDoesNotThrow(() -> validator.validate(root, new FlowVariableSchema()));
        rule.setField("unknown");
        assertThrows(BizException.class, () -> validator.validate(root, new FlowVariableSchema()));
        rule.setField("amount");
        assertDoesNotThrow(() -> validator.validate(root, schema()));
        var permission = new FlowFieldPermission();
        permission.setField("PROCESS_START_USER_ID");
        root.setFieldsPermission(List.of(permission));
        assertThrows(BizException.class, () -> validator.validate(root, schema()));
    }

    @Test
    void shouldValidateConditionTypesInBothChildAndBranchNodes() {
        var validator = validator();
        var root = node("root", 1);
        var child = node("child", 3);
        var branch = node("branch", 3);
        root.setChildNode(child);
        root.setConditionNodes(List.of(branch));
        var childRule = conditionRule(child, "amount", 0);
        var branchRule = conditionRule(branch, "amount", 0);
        assertEquals("条件变量类型不匹配：amount",
                assertThrows(BizException.class, () -> validator.validate(root, schema())).getMessage());
        childRule.setType(1);
        assertThrows(BizException.class, () -> validator.validate(root, schema()));
        branchRule.setType(1);
        assertDoesNotThrow(() -> validator.validate(root, schema()));
        branchRule.setField("PROCESS_START_USER_ID");
        branchRule.setType(3);
        assertDoesNotThrow(() -> validator.validate(root, schema()));
    }

    private ProcessConditionRule conditionRule(FlowNode node, String field, int type) {
        var rule = new ProcessConditionRule();
        rule.setField(field);
        rule.setType(type);
        var group = new ProcessConditionGroup();
        group.setConditionRules(List.of(rule));
        var condition = new ProcessNodeCondition();
        condition.setConditionGroups(List.of(group));
        node.setCondition(condition);
        return rule;
    }

    private FlowVariableSchema schema() {
        var field = new FlowVariableSchema.Field();
        field.setField("amount");
        field.setType(1);
        var schema = new FlowVariableSchema();
        schema.setFields(List.of(field));
        return schema;
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
