package com.plasticene.boot.flow.core.api.param;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 使用 FLOW 自带表单发起流程的参数。
 * @author ZFJ
 * @since 2026-11-16
 */
@Data
@Schema(description = "FLOW 自带表单流程发起参数")
public class FlowStartParam {
    @Schema(description = "发布定义标识")
    @NotNull(message = "发布定义标识不能为空")
    private Long definitionId;
    @Schema(description = "本次申请的审批数据")
    @JsonDeserialize(using = FlowVariableMapDeserializer.class)
    private Map<String, Object> varMap;
}
