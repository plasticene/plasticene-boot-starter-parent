package com.plasticene.boot.flow.core.api.param;

import lombok.Data;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 按发布定义 ID 发起业务审批的参数。
 * @author ZFJ
 * @since 2026-11-16
 */
@Data
@Schema(description = "按发布定义 ID 发起业务审批的参数")
public class FlowStartByIdParam {
    @Schema(description = "发布定义标识")
    @NotNull(message = "发布定义标识不能为空")
    private Long definitionId;
    @Schema(description = "业务申请记录标识")
    @NotBlank(message = "业务申请记录标识不能为空")
    private String businessId;
    @Schema(description = "本次申请的审批数据")
    private Map<String, Object> varMap;
}
