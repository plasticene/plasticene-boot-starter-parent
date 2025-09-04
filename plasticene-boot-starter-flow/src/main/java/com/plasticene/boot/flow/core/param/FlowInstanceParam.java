package com.plasticene.boot.flow.core.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ZFJ
 * @date 2025/9/4
 */
@Data
public class FlowInstanceParam {

    @Schema(description = "流程模型id")
    private Long processId;
    @Schema(description = "业务id")
    private Long businessId;
}
