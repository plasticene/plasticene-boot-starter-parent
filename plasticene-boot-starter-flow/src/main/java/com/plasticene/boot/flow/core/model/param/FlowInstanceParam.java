package com.plasticene.boot.flow.core.model.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * @author ZFJ
 * @date 2025/9/4
 */
@Data
public class FlowInstanceParam {

    @Schema(description = "流程模型id")
    @NotNull(message = "流程id不能为空")
    private Long definitionId;
    @Schema(description = "业务id")
    private Long businessId;
    @Schema(description = "变量值")
    private Map<String, Object> varMap;
}
