package com.plasticene.boot.flow.core.api.event;

/**
 * 审批待办失效原因。
 * @author ZFJ
 * @since 2026-11-06
 */
public enum FlowTaskCancelReason {
    /** 流程实例审批拒绝，剩余待办失效。 */
    INSTANCE_REJECTED,
    /** 发起人取消流程实例，剩余待办失效。 */
    INSTANCE_CANCELLED,
    /** 或签节点或加签组已完成，其他待办无需继续办理。 */
    NODE_COMPLETED,
    /** 流程退回，重走路径中的原待办失效。 */
    RETURN,
    /** 审批人撤回审批结果，受影响的后续待办失效。 */
    WITHDRAW,
    /** 原任务等待前加签或后加签完成，暂时不可办理。 */
    ADD_SIGN_WAITING
}
