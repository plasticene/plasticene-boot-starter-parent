package com.plasticene.boot.flow.core.model;

import java.util.List;

/**
 * 流程模型中的节点结构。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public record FlowNode(String key, String name, FlowEnums.NodeType type, FlowEnums.ApprovalMode approvalMode,
                       boolean requireComment, List<String> configuredAssignees, FlowNode childNode,
                       List<FlowBranch> branches) {

    public FlowNode(String key, String name, FlowEnums.NodeType type, FlowEnums.ApprovalMode approvalMode,
                    boolean requireComment, List<String> configuredAssignees, FlowNode childNode) {
        this(key, name, type, approvalMode, requireComment, configuredAssignees, childNode, List.of());
    }

    public FlowNode {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("node key must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("node name must not be blank");
        }
        configuredAssignees = configuredAssignees == null ? List.of() : List.copyOf(configuredAssignees);
        branches = branches == null ? List.of() : List.copyOf(branches);
    }
}
