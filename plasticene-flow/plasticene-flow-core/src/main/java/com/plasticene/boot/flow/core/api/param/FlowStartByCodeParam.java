package com.plasticene.boot.flow.core.api.param;

import lombok.Data;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 按流程编码发起业务审批的参数。
 * @author ZFJ
 * @since 2026-11-16
 */
@Data
@Schema(description = "按流程编码发起业务审批的参数")
public class FlowStartByCodeParam {
    @Schema(description = "流程编码")
    @NotBlank(message = "流程编码不能为空")
    private String processCode;
    @Schema(description = "业务申请记录标识")
    @NotBlank(message = "业务申请记录标识不能为空")
    private String businessId;
    @Schema(description = "本次申请的审批数据")
    private Map<String, Object> varMap;
}
