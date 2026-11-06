package com.plasticene.boot.flow.core.event;

/**
 * 审批任务完成动作。
 * @author ZFJ
 * @since 2026-11-06
 */
public enum FlowTaskAction {
    /** 任务审批通过，包含人工审批和系统自动通过。 */
    APPROVE,
    /** 任务审批拒绝，包含人工审批和系统自动拒绝。 */
    REJECT,
    /** 任务已转交，接收人通过新任务继续办理。 */
    TRANSFER,
    /** 任务已退回至指定的前序审批节点。 */
    RETURN
}
