package com.plasticene.boot.flow.core.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 保存表单字段变化及历史展示文本。
 * @author ZFJ
 * @since 2026-11-04
 */
@Data
@Schema(description = "表单字段变更明细")
public class FlowFormFieldChange {
    @Schema(description = "字段标识")
    private String field;
    @Schema(description = "字段名称快照")
    private String title;
    @Schema(description = "修改前原始值")
    private Object oldValue;
    @Schema(description = "修改后原始值")
    private Object newValue;
    @Schema(description = "修改前展示文本快照")
    private String oldText;
    @Schema(description = "修改后展示文本快照")
    private String newText;
}
