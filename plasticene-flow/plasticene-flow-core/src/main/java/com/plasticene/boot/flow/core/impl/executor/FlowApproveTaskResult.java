package com.plasticene.boot.flow.core.impl.executor;

/**
 * 审批任务创建结果，供执行器决定节点是否继续流转。
 * @author ZFJ
 * @since 2026-11-10
 */
public enum FlowApproveTaskResult {
    /** 本次不推进，可能已创建待办，也可能因无人处理而停留。 */
    WAITING,
    /** 当前节点已通过，由执行器继续流转。 */
    APPROVED,
    /** 当前节点已拒绝，由执行器结束实例。 */
    REJECTED
}
