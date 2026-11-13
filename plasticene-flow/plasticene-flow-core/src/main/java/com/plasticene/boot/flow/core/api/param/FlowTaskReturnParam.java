package com.plasticene.boot.flow.core.api.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交当前审批待办的退回目标及原因。
 * @author ZFJ
 * @since 2026-11-02
 */
@Data
public class FlowTaskReturnParam {
    @NotNull(message = "任务id不能为空")
    @Schema(description = "当前待办任务id")
    private Long taskId;
    @NotBlank(message = "退回目标不能为空")
    @Schema(description = "退回目标节点key")
    private String targetNodeKey;
    @NotBlank(message = "退回原因不能为空")
    @Schema(description = "退回原因，去除首尾空白后1至500字")
    private String comment;
}
