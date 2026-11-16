package com.plasticene.boot.flow.core.api.param;

import lombok.Data;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 定义流程发起或路径预估的公开参数。
 * @author ZFJ
 * @since 2026-11-16
 */
@Data
@Schema(description = "FlowRoutePreviewParam")
public class FlowRoutePreviewParam {
    @Schema(description = "发布定义标识")
    @jakarta.validation.constraints.NotNull
    private Long definitionId;
    @Schema(description = "本次申请的审批数据")
    private Map<String, Object> varMap;
}
