package com.plasticene.boot.flow.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ZFJ
 * @since 2026/4/13
 */
@Data
public class FlowModelStatisticsVO {
    @Schema(description = "流程模型-总数")
    private Long total;
    @Schema(description = "流程模型-已发布的")
    private Long published;
    @Schema(description = "流程模型-运行中的")
    private Long running;
    @Schema(description = "流程模型-已停止的")
    private Long stop;

}
