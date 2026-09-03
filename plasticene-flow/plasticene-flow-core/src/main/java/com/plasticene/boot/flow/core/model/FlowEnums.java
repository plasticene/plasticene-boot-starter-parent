package com.plasticene.boot.flow.core.model;

/**
 * 审批流领域枚举集合。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public final class FlowEnums {

    private FlowEnums() {
    }

    public enum ModelStatus {
        DRAFT, PUBLISHED, DISABLED
    }

    public enum NodeType {
        START, APPROVAL, COPY, CONDITION, END
    }

    public enum ApprovalMode {
        ALL, ANY, ORDER
    }

    public enum InstanceStatus {
        RUNNING, APPROVED, REJECTED, CANCELLED
    }

    public enum TaskStatus {
        RUNNING, APPROVED, REJECTED, RETURNED, CANCELLED
    }

    public enum LogicalOperator {
        AND, OR
    }

    public enum ComparisonOperator {
        EQ, NE, GT, GE, LT, LE, IN, NOT_IN, CONTAINS, NOT_EMPTY, EMPTY
    }
}
