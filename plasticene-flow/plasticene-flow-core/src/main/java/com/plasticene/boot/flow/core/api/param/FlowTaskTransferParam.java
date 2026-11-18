package com.plasticene.boot.flow.core.api.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 审批任务转交参数。
 * @author ZFJ
 * @since 2026-11-02
 */
@Data
public class FlowTaskTransferParam {
    @Schema(description = "当前用户待转交的审批任务ID")
    @NotNull(message = "任务ID不能为空")
    private Long taskId;
    @Schema(description = "同组织启用的接收人ID，不能为当前用户或当前节点已有审批人")
    @NotNull(message = "接收人ID不能为空")
    private Long assignee;
    @Schema(description = "必填转交原因，最多500字")
    @NotBlank(message = "转交原因不能为空")
    @Size(max = 500)
    private String comment;
}
