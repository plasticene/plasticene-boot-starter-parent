package com.plasticene.boot.flow.core.model.vo;

import com.plasticene.boot.flow.core.model.dto.FlowNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author ZFJ
 * @since 2026/9/15
 */
@Data
@Schema(description = "流程定义VO")
public class FlowDefinitionVO {

    @Schema(description = "流程模型id")
    private Long modelId;
    @Schema(description = "发布定义id")
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
    @Schema(description = "发布版本")
    private Integer version;
    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
    @Schema(description = "发布时的表单配置快照")
    private Map<String, Object> formConfig;
    @Schema(description = "发布时的流程模型快照")
    private FlowNode modelNode;

}
