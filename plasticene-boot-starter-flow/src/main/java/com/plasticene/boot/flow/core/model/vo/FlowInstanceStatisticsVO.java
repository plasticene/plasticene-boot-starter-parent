package com.plasticene.boot.flow.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程实例状态统计信息
 *
 * @author ZFJ
 * @since 2026-09-17
 */
@Data
@Schema(description = "流程实例状态统计信息")
public class FlowInstanceStatisticsVO {

    @Schema(description = "实例总数")
    private Long total;

    @Schema(description = "审批中实例数")
    private Long running;

    @Schema(description = "审批通过实例数")
    private Long approved;

    @Schema(description = "审批拒绝实例数")
    private Long rejected;

    @Schema(description = "已取消实例数")
    private Long cancelled;
}
