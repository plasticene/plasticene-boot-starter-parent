package com.plasticene.boot.flow.core.model;

/**
 * 条件节点的一条候选分支。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public record FlowBranch(String name, FlowCondition condition, boolean defaultBranch, FlowNode childNode) {

    public FlowBranch {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("branch name must not be blank");
        }
        if (childNode == null) {
            throw new IllegalArgumentException("branch childNode must not be null");
        }
        if (!defaultBranch && condition == null) {
            throw new IllegalArgumentException("non-default branch condition must not be null");
        }
    }
}
