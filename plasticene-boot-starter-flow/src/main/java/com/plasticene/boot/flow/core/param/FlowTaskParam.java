package com.plasticene.boot.flow.core.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author ZFJ
 * @date 2025/9/5
 */
@Data
public class FlowTaskParam {
    @NotNull(message = "taskId不能为空")
    @Schema(description = "任务id")
    private Long taskId;
    @Schema(description = "审批意见")
    private String comment;
}
