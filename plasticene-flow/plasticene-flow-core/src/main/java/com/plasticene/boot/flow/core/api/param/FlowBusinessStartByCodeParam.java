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
@Schema(description = "FlowBusinessStartByCodeParam")
public class FlowBusinessStartByCodeParam {
    @Schema(description = "流程编码")
    @jakarta.validation.constraints.NotBlank
    private String processCode;
    @Schema(description = "业务申请记录标识")
    @jakarta.validation.constraints.NotBlank
    private String businessId;
    @Schema(description = "本次申请的审批数据")
    private Map<String, Object> varMap;
}
