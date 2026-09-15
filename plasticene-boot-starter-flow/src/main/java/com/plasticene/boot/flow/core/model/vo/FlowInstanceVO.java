package com.plasticene.boot.flow.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前用户发起的流程实例列表信息
 *
 * @author ZFJ
 * @since 2026-09-15
 */
@Data
@Schema(description = "流程实例VO")
public class FlowInstanceVO {

    @Schema(description = "流程实例id")
    private Long id;

    @Schema(description = "流程发布定义id")
    private Long definitionId;

    @Schema(description = "业务id")
    private Long businessId;

    @Schema(description = "流程名称")
    private String name;

    @Schema(description = "流程编码")
    private String code;

    @Schema(description = "流程分类编码")
    private String category;

    @Schema(description = "流程分类名称")
    private String categoryName;

    @Schema(description = "当前节点key")
    private String currentNodeKey;

    @Schema(description = "当前节点名称")
    private String currentNodeName;

    @Schema(description = "实例状态 0：审批中 1：审批通过 2：审批拒绝 3：已取消")
    private Integer status;

    @Schema(description = "实例状态名称")
    private String statusName;

    @Schema(description = "是否可以取消")
    private Boolean cancelable;

    @Schema(description = "发起时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
