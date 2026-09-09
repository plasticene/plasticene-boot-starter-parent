package com.plasticene.boot.flow.core.model.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 开关流程模型参数
 * @author ZFJ
 * @since 2026/9/4
 */
@Data
@Schema(description = "开关流程模型参数")
public class FlowModelEnableParam {
    @Schema(description = "流程模型ID")
    @NotNull(message = "流程模型ID不能为空")
    private Long id;
    @Schema(description = "是否启用 1:启用 2:禁用")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
