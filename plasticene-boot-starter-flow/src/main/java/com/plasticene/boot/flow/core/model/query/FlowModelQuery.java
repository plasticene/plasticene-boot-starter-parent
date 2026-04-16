package com.plasticene.boot.flow.core.model.query;

import com.plasticene.boot.common.pojo.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 *
 * <p> 工作流-流程模型表 </p>
 *
 * @author ZFJ
 * @since 2026-04-13
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工作流-流程模型表")
public class FlowModelQuery extends PageQuery {
    @Schema(description = "关键字, 搜索名称或者code")
    private String keyword;
    @Schema(description = "流程分类")
    private String category;
    @Schema(description = "流程状态  0：草稿  1：发布  -1：停用")
    private Integer status;
}