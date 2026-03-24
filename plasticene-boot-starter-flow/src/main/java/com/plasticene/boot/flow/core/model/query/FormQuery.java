package com.plasticene.boot.flow.core.model.query;

import com.plasticene.boot.common.pojo.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ZFJ
 * @since 2026/2/25
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FormQuery extends PageQuery {
    @Schema(description = "表单名称")
    private String name;
}
