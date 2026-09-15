package com.plasticene.boot.flow.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程实例任务记录
 *
 * @author ZFJ
 * @since 2026-09-15
 */
@Data
@Schema(description = "流程实例任务记录")
public class FlowTaskVO {

    @Schema(description = "任务id")
    private Long id;
    @Schema(description = "节点key")
    private String nodeKey;
    @Schema(description = "节点名称")
    private String nodeName;
    @Schema(description = "节点类型")
    private Integer nodeType;
    @Schema(description = "处理人id")
    private Long assignee;
    @Schema(description = "处理人名称")
    private String assigneeName;
    @Schema(description = "审批类型")
    private Integer approveType;
    @Schema(description = "多人审批方式")
    private Integer approveMode;
    @Schema(description = "任务状态 0：处理中 1：已完成 2：拒绝 3：已取消")
    private Integer status;
    @Schema(description = "任务状态名称")
    private String statusName;
    @Schema(description = "开始时间")
    private LocalDateTime startTime;
    @Schema(description = "结束时间")
    private LocalDateTime endTime;
    @Schema(description = "审批意见")
    private String comment;
    @Schema(description = "耗时，单位毫秒")
    private Long durationMillis;
}
