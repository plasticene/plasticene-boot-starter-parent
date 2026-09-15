package com.plasticene.boot.flow.core.model.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 流程预计路径计算参数
 *
 * @author ZFJ
 * @since 2026-09-14
 */
@Data
@Schema(description = "流程预计路径计算参数")
public class FlowRouteParam {
    @NotNull(message = "流程发布id不能为空")
    @Schema(description = "流程发布id")
    private Long definitionId;
    @Schema(description = "表单变量")
    private Map<String, Object> varMap;
}
