package com.plasticene.boot.flow.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程预计路径节点
 *
 * @author ZFJ
 * @since 2026-09-14
 */
@Data
@Schema(description = "流程预计路径节点")
public class FlowRouteNodeVO {
    @Schema(description = "节点key")
    private String key;
    @Schema(description = "节点名称")
    private String name;
    @Schema(description = "节点类型")
    private Integer type;
    @Schema(description = "节点展示内容")
    private String showText;
    @Schema(description = "多人审批方式")
    private Integer approveMode;
}
