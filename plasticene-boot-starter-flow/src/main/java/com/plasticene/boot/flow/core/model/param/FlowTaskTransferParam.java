package com.plasticene.boot.flow.core.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 审批任务转交参数。
 * @author ZFJ
 * @since 2026-11-02
 */
@Data
public class FlowTaskTransferParam {
    /** 当前用户待转交的审批任务ID。 */
    @NotNull
    @Positive
    private Long taskId;
    /** 同组织启用的接收人ID，不能为当前用户或当前节点已有审批人。 */
    @NotNull
    @Positive
    private Long assignee;
    /** 必填转交原因，最多500字。 */
    @NotBlank
    @Size(max = 500)
    private String comment;
}
