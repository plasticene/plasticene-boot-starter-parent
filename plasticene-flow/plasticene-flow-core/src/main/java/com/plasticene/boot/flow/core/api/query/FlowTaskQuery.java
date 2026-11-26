package com.plasticene.boot.flow.core.api.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.plasticene.boot.common.pojo.PageQuery;
import com.plasticene.boot.flow.core.api.param.FlowBusinessFieldFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 当前用户流程任务分页查询参数
 *
 * @author ZFJ
 * @since 2026-09-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "当前用户流程任务分页查询参数")
public class FlowTaskQuery extends PageQuery {

    @Schema(description = "关键字，搜索流程名称或者编码")
    private String keyword;

    @Schema(description = "流程分类编码")
    private String category;

    @Schema(description = "任务状态 0：处理中 1：已完成 2：拒绝 3：已取消")
    private List<Integer> statuses;

    @Schema(description = "流程实例状态 0：审批中 1：审批通过 2：审批拒绝 3：已取消")
    private List<Integer> instanceStatuses;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "任务接收时间开始值")
    private LocalDateTime taskStartTimeBegin;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "任务接收时间结束值")
    private LocalDateTime taskStartTimeEnd;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "任务完成时间开始值")
    private LocalDateTime taskEndTimeBegin;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "任务完成时间结束值")
    private LocalDateTime taskEndTimeEnd;

    @Schema(description = "处理人id", hidden = true)
    private Long assignee;

    @Schema(description = "租户id", hidden = true)
    private Long orgId;

    @Schema(description = "节点类型", hidden = true)
    private Integer nodeType;

    @Schema(description = "审批列表业务字段条件")
    private List<FlowBusinessFieldFilter> businessFilters;
}
