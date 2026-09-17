package com.plasticene.boot.flow.core.model.query;

import com.plasticene.boot.flow.core.enums.FlowDashboardGranularityEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 流程数据总览查询参数
 *
 * @author ZFJ
 * @since 2026-09-17
 */
@Data
@Schema(description = "流程数据总览查询参数")
public class FlowDashboardQuery {

    @NotNull(message = "统计开始时间不能为空")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "统计开始时间，包含该时间")
    private LocalDateTime startTime;

    @NotNull(message = "统计结束时间不能为空")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "统计结束时间，不包含该时间")
    private LocalDateTime endTime;

    @NotNull(message = "趋势时间粒度不能为空")
    @Schema(description = "趋势时间粒度：DAY、WEEK、MONTH")
    private FlowDashboardGranularityEnum granularity;

    @Schema(description = "租户id", hidden = true)
    private Long orgId;
}
