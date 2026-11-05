package com.plasticene.boot.flow.core.model.query;

import com.plasticene.boot.common.pojo.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 接收流程表单分页查询条件。
 * @author ZFJ
 * @since 2026-11-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FlowFormQuery extends PageQuery {
    @Schema(description = "表单名称")
    private String name;
    @Schema(description = "表单分类编码，关联流程分类")
    private String category;
}
