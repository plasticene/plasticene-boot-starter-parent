package com.plasticene.boot.flow.core.impl.model.query;

import com.plasticene.boot.common.pojo.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ZFJ
 * @since 2026/2/9
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FlowCategoryQuery extends PageQuery {
    @Schema(description = "分类名称，模糊匹配")
    private String name;
    @Schema(description = "分类标识，不区分大小写精确匹配")
    private String code;
}
