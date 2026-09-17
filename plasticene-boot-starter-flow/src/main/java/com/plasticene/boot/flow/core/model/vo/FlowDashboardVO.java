package com.plasticene.boot.flow.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 流程数据总览
 *
 * @author ZFJ
 * @since 2026-09-17
 */
@Data
@Schema(description = "流程数据总览")
public class FlowDashboardVO {

    @Schema(description = "核心指标")
    private Summary summary;

    @Schema(description = "流程吞吐趋势")
    private List<TrendItem> trends;

    @Schema(description = "实时积压结构")
    private Backlog backlog;

    @Schema(description = "流程健康排行")
    private List<ModelRanking> modelRankings;

    @Schema(description = "节点瓶颈排行")
    private List<NodeRanking> nodeRankings;

    @Data
    @Schema(description = "流程数据总览核心指标")
    public static class Summary {
        @Schema(description = "当前统计时间范围内发起的流程实例数量")
        private Long started;

        @Schema(description = "上一对比周期内发起的流程实例数量")
        private Long previousStarted;

        @Schema(description = "当前统计时间范围内完成的流程实例数量，包含通过、拒绝和取消")
        private Long completed;

        @Schema(description = "上一对比周期内完成的流程实例数量，包含通过、拒绝和取消")
        private Long previousCompleted;

        @Schema(description = "当前处于处理中的流程实例数量，为实时存量，不受统计时间范围影响")
        private Long currentRunning;

        @Schema(description = "当前统计时间范围内审批通过的流程实例数量")
        private Long approved;

        @Schema(description = "当前统计时间范围内审批拒绝的流程实例数量")
        private Long rejected;

        @Schema(description = "当前统计时间范围内已取消的流程实例数量")
        private Long cancelled;

        @Schema(description = "当前处理时长超过3天的流程实例数量，为实时存量")
        private Long longRunning;

        @Schema(description = "审批通过率，单位为百分比，计算公式为通过数/(通过数+拒绝数)")
        private BigDecimal approvalRate;

        @Schema(description = "当前统计时间范围内已通过或已拒绝流程实例处理时长的P50（中位数），单位为秒")
        private Long medianDurationSeconds;

        @Schema(description = "上一对比周期内已通过或已拒绝流程实例处理时长的P50（中位数），单位为秒")
        private Long previousMedianDurationSeconds;

        @Schema(description = "当前统计时间范围内已通过或已拒绝流程实例处理时长的P90，单位为秒")
        private Long p90DurationSeconds;
    }

    @Data
    @Schema(description = "流程吞吐趋势数据点")
    public static class TrendItem {
        @Schema(description = "趋势统计分桶日期；按日时为当天，按周时为周一，按月时为当月1日")
        private LocalDate date;

        @Schema(description = "当前趋势分桶内发起的流程实例数量")
        private Long started;

        @Schema(description = "当前趋势分桶内完成的流程实例数量，包含通过、拒绝和取消")
        private Long completed;
    }

    @Data
    @Schema(description = "实时积压结构")
    public static class Backlog {
        @Schema(description = "当前处理时长小于1天的流程实例数量")
        private Long withinOneDay;

        @Schema(description = "当前处理时长大于等于1天且小于3天的流程实例数量")
        private Long oneToThreeDays;

        @Schema(description = "当前处理时长大于等于3天的流程实例数量")
        private Long overThreeDays;
    }

    @Data
    @Schema(description = "流程健康排行")
    public static class ModelRanking {
        @Schema(description = "流程模型ID")
        private Long modelId;

        @Schema(description = "流程模型名称")
        private String processName;

        @Schema(description = "当前统计时间范围内该流程模型发起的实例数量")
        private Long started;

        @Schema(description = "该流程模型当前处于处理中的实例数量，为实时存量")
        private Long currentRunning;

        @Schema(description = "当前统计时间范围内该流程模型审批通过的实例数量")
        private Long approved;

        @Schema(description = "当前统计时间范围内该流程模型审批拒绝的实例数量")
        private Long rejected;

        @Schema(description = "该流程模型审批通过率，单位为百分比，计算公式为通过数/(通过数+拒绝数)")
        private BigDecimal approvalRate;

        @Schema(description = "当前统计时间范围内该流程模型已通过或已拒绝实例处理时长的P50（中位数），单位为秒")
        private Long medianDurationSeconds;

        @Schema(description = "当前统计时间范围内该流程模型已通过或已拒绝实例处理时长的P90，单位为秒")
        private Long p90DurationSeconds;

        @Schema(description = "该流程模型当前处理时长超过3天的实例数量，为实时存量")
        private Long longRunning;
    }

    @Data
    @Schema(description = "节点瓶颈排行")
    public static class NodeRanking {
        @Schema(description = "流程模型ID")
        private Long modelId;

        @Schema(description = "流程模型名称")
        private String processName;

        @Schema(description = "流程节点唯一标识")
        private String nodeKey;

        @Schema(description = "流程节点名称")
        private String nodeName;

        @Schema(description = "该节点当前待处理的任务数量，为实时存量")
        private Long pendingCount;

        @Schema(description = "该节点当前待处理任务中的最长等待时长，单位为秒")
        private Long maxWaitingSeconds;

        @Schema(description = "当前统计时间范围内该节点已完成任务处理时长的P90，单位为秒")
        private Long p90DurationSeconds;
    }
}
