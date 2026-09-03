package com.plasticene.boot.flow.engine;

import com.plasticene.boot.flow.core.FlowException;
import com.plasticene.boot.flow.core.model.FlowBranch;
import com.plasticene.boot.flow.core.model.FlowCondition;
import com.plasticene.boot.flow.core.model.FlowEnums;
import com.plasticene.boot.flow.core.model.FlowNode;

import java.util.HashSet;
import java.util.Set;

/**
 * 流程模型结构与节点配置校验器。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public class FlowModelValidator {

    public void validate(FlowNode rootNode) {
        if (rootNode == null) {
            throw invalid("Root node is required");
        }
        if (rootNode.type() != FlowEnums.NodeType.START) {
            throw invalid("The root node must be START");
        }
        Set<String> nodeKeys = new HashSet<>();
        ValidationState state = new ValidationState();
        validateNode(rootNode, true, nodeKeys, state);
        if (!state.approvalFound) {
            throw invalid("At least one approval node is required");
        }
        if (!state.endFound) {
            throw invalid("Flow must end with an END node");
        }
    }

    private void validateNode(FlowNode node, boolean root, Set<String> nodeKeys, ValidationState state) {
        if (node == null) {
            throw invalid("Every branch must end with an END node");
        }
        if (node.type() == null) {
            throw invalid("Node type is required: " + node.key());
        }
        if (!nodeKeys.add(node.key())) {
            throw invalid("Duplicate or cyclic node key: " + node.key());
        }
        if (!root && node.type() == FlowEnums.NodeType.START) {
            throw invalid("START node can only be the root node");
        }
        if (node.type() == FlowEnums.NodeType.APPROVAL) {
            state.approvalFound = true;
            if (node.approvalMode() == null) {
                throw invalid("Approval mode is required: " + node.key());
            }
        }
        if (node.type() == FlowEnums.NodeType.CONDITION) {
            validateConditionNode(node, nodeKeys, state);
            return;
        }
        if (!node.branches().isEmpty()) {
            throw invalid("Only CONDITION nodes can contain branches: " + node.key());
        }
        if (node.type() == FlowEnums.NodeType.END) {
            state.endFound = true;
            if (node.childNode() != null) {
                throw invalid("END node cannot have a child");
            }
            return;
        }
        validateNode(node.childNode(), false, nodeKeys, state);
    }

    private void validateConditionNode(FlowNode node, Set<String> nodeKeys, ValidationState state) {
        if (node.childNode() != null || node.branches().isEmpty()) {
            throw invalid("CONDITION node must contain branches and no direct child: " + node.key());
        }
        long defaults = node.branches().stream().filter(FlowBranch::defaultBranch).count();
        if (defaults > 1) {
            throw invalid("CONDITION node can contain at most one default branch: " + node.key());
        }
        for (FlowBranch branch : node.branches()) {
            if (!branch.defaultBranch()) {
                validateCondition(branch.condition(), node.key());
            }
            validateNode(branch.childNode(), false, nodeKeys, state);
        }
    }

    private void validateCondition(FlowCondition condition, String nodeKey) {
        if (condition == null || condition.groups().isEmpty()
                || condition.groups().stream().anyMatch(group -> group.rules().isEmpty())) {
            throw invalid("Condition groups and rules must not be empty: " + nodeKey);
        }
    }

    private FlowException invalid(String message) {
        return new FlowException("FLOW_MODEL_INVALID", message);
    }

    private static final class ValidationState {
        private boolean approvalFound;
        private boolean endFound;
    }
}
