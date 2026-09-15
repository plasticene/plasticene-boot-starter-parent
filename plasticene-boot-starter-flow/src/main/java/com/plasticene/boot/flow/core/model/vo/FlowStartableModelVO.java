package com.plasticene.boot.flow.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 可发起的流程模型摘要
 *
 * @author ZFJ
 * @since 2026-09-14
 */
@Data
@Schema(description = "可发起的流程模型摘要")
public class FlowStartableModelVO {
    @Schema(description = "流程模型id")
    private Long modelId;
    @Schema(description = "当前发布定义id")
    private Long definitionId;
    @Schema(description = "流程编码")
    private String code;
    @Schema(description = "流程名称")
    private String name;
    @Schema(description = "流程分类编码")
    private String category;
    @Schema(description = "流程分类名称")
    private String categoryName;
    @Schema(description = "流程说明")
    private String remark;
    @Schema(description = "当前发布版本")
    private Integer version;
    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
    @Schema(description = "当前用户最近发起时间")
    private LocalDateTime lastStartTime;
}
