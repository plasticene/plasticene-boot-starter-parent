package com.plasticene.boot.flow.core.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 返回审批列表行中的业务字段值。
 * @author ZFJ
 * @since 2026-11-20
 */
public record FlowBusinessFieldValue(
        @Schema(description = "字段值") String value,
        @Schema(description = "发布定义中的选项标签") String label) {
}
