package com.plasticene.boot.flow.core.api.param;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 表示审批列表的一项业务字段条件。
 * @author ZFJ
 * @since 2026-11-20
 */
public record FlowBusinessFieldFilter(
        @Schema(description = "字段标识") String field,
        @Schema(description = "操作符") String operator,
        @Schema(description = "条件值或区间开始值") String value,
        @Schema(description = "区间结束值") String endValue) {
}
