package com.plasticene.boot.flow.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前用户流程任务分页信息
 *
 * @author ZFJ
 * @since 2026-09-16
 */
@Data
@Schema(description = "当前用户流程任务分页信息")
public class FlowTaskPageVO {

    @Schema(description = "任务id")
    private Long taskId;

    @Schema(description = "流程实例id")
    private Long instanceId;

    @Schema(description = "流程发布定义id")
    private Long definitionId;

    @Schema(description = "业务id")
    private Long businessId;

    @Schema(description = "流程名称")
    private String processName;

    @Schema(description = "流程编码")
    private String processCode;

    @Schema(description = "流程分类编码")
    private String category;

    @Schema(description = "流程分类名称")
    private String categoryName;

    @Schema(description = "节点key")
    private String nodeKey;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "流程发起人id")
    private Long startUserId;

    @Schema(description = "流程发起人名称")
    private String startUserName;

    @Schema(description = "流程实例发起时间")
    private LocalDateTime instanceStartTime;

    @Schema(description = "任务接收时间")
    private LocalDateTime taskStartTime;

    @Schema(description = "任务完成时间")
    private LocalDateTime taskEndTime;

    @Schema(description = "任务耗时，单位秒")
    private Long elapsedTime;

    @Schema(description = "任务状态 0：处理中 1：已完成 2：拒绝 3：已取消")
    private Integer status;

    @Schema(description = "审批意见")
    private String comment;

    @Schema(description = "是否需要填写审批意见 0：否 1：是")
    private Integer requireComment;
}
