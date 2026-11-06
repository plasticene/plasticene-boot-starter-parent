package com.plasticene.boot.flow.core.event;

/**
 * 流程事件类型。
 * @author ZFJ
 * @since 2026-11-06
 */
public enum FlowEventType {
    /** 流程实例已创建，开始执行节点。 */
    INSTANCE_STARTED,
    /** 整个流程实例审批通过。 */
    INSTANCE_APPROVED,
    /** 整个流程实例审批拒绝。 */
    INSTANCE_REJECTED,
    /** 流程实例已被发起人取消。 */
    INSTANCE_CANCELLED,
    /** 审批任务已生效，处理人可以办理；包含恢复生效的任务。 */
    TASK_CREATED,
    /** 审批任务已处理完成，具体动作见 FlowTaskAction。 */
    TASK_COMPLETED,
    /** 审批待办已失效或暂停办理，具体原因见 FlowTaskCancelReason。 */
    TASK_CANCELLED
}
