package com.plasticene.boot.flow.core.service.impl;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.dao.FlowDashboardDAO;
import com.plasticene.boot.flow.core.enums.FlowDashboardGranularityEnum;
import com.plasticene.boot.flow.core.model.query.FlowDashboardQuery;
import com.plasticene.boot.flow.core.model.vo.FlowDashboardVO;
import com.plasticene.boot.flow.core.service.FlowDashboardService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程数据总览服务实现
 *
 * @author ZFJ
 * @since 2026-09-17
 */
@Service
public class FlowDashboardServiceImpl implements FlowDashboardService {

    private static final int MAX_RANGE_DAYS = 366;
    private static final int LONG_RUNNING_DAYS = 3;

    @Resource
    private FlowDashboardDAO flowDashboardDAO;

    @Override
    public FlowDashboardVO overview(FlowDashboardQuery query) {
        validateQuery(query);
        LocalDateTime now = LocalDateTime.now();
        // 计算当前查询时间范围的时长，用于推导上一个同等时长周期的起始时间（如环比分析）
        Duration range = Duration.between(query.getStartTime(), query.getEndTime());
        LocalDateTime previousStartTime = query.getStartTime().minus(range);
        LocalDateTime longRunningBefore = now.minusDays(LONG_RUNNING_DAYS);

        FlowDashboardVO.Summary summary = flowDashboardDAO.selectSummary(
                query, previousStartTime, longRunningBefore);
        FlowDashboardVO.Summary duration = flowDashboardDAO.selectDurationStatistics(
                query.getOrgId(), query.getStartTime(), query.getEndTime());
        FlowDashboardVO.Summary previousDuration = flowDashboardDAO.selectDurationStatistics(
                query.getOrgId(), previousStartTime, query.getStartTime());
        summary.setApprovalRate(calculateRate(summary.getApproved(), summary.getRejected()));
        summary.setMedianDurationSeconds(valueOrZero(duration.getMedianDurationSeconds()));
        summary.setP90DurationSeconds(valueOrZero(duration.getP90DurationSeconds()));
        summary.setPreviousMedianDurationSeconds(valueOrZero(previousDuration.getMedianDurationSeconds()));

        List<FlowDashboardVO.ModelRanking> modelRankings = flowDashboardDAO.listModelRankings(
                query, longRunningBefore);
        modelRankings.forEach(item -> item.setApprovalRate(
                calculateRate(item.getApproved(), item.getRejected())));

        FlowDashboardVO dashboard = new FlowDashboardVO();
        dashboard.setSummary(summary);
        dashboard.setTrends(fillTrendGaps(query, flowDashboardDAO.listTrends(query)));
        dashboard.setBacklog(flowDashboardDAO.selectBacklog(
                query.getOrgId(), now.minusDays(1), longRunningBefore));
        dashboard.setModelRankings(modelRankings);
        dashboard.setNodeRankings(flowDashboardDAO.listNodeRankings(query, now));
        return dashboard;
    }

    private void validateQuery(FlowDashboardQuery query) {
        if (query.getOrgId() == null) {
            throw new BizException("租户信息不能为空");
        }
        if (query.getStartTime() == null || query.getEndTime() == null || query.getGranularity() == null) {
            throw new BizException("统计时间范围和时间粒度不能为空");
        }
        if (!query.getEndTime().isAfter(query.getStartTime())) {
            throw new BizException("统计结束时间必须晚于开始时间");
        }
        if (Duration.between(query.getStartTime(), query.getEndTime()).toDays() > MAX_RANGE_DAYS) {
            throw new BizException("统计时间范围不能超过366天");
        }
    }

    private List<FlowDashboardVO.TrendItem> fillTrendGaps(
            FlowDashboardQuery query, List<FlowDashboardVO.TrendItem> source) {
        Map<LocalDate, FlowDashboardVO.TrendItem> sourceMap = new LinkedHashMap<>();
        source.forEach(item -> sourceMap.put(item.getDate(), item));

        LocalDate cursor = firstBucket(query.getStartTime().toLocalDate(), query.getGranularity());
        LocalDate endDate = query.getEndTime().toLocalDate();
        List<FlowDashboardVO.TrendItem> result = new ArrayList<>();
        while (cursor.isBefore(endDate)) {
            FlowDashboardVO.TrendItem item = sourceMap.get(cursor);
            if (item == null) {
                item = new FlowDashboardVO.TrendItem();
                item.setDate(cursor);
                item.setStarted(0L);
                item.setCompleted(0L);
            }
            result.add(item);
            cursor = nextBucket(cursor, query.getGranularity());
        }
        return result;
    }

    private LocalDate firstBucket(LocalDate date, FlowDashboardGranularityEnum granularity) {
        return switch (granularity) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    private LocalDate nextBucket(LocalDate date, FlowDashboardGranularityEnum granularity) {
        return switch (granularity) {
            case DAY -> date.plusDays(1);
            case WEEK -> date.plusWeeks(1);
            case MONTH -> date.plusMonths(1);
        };
    }

    private BigDecimal calculateRate(Long approved, Long rejected) {
        long approvedCount = valueOrZero(approved);
        long total = approvedCount + valueOrZero(rejected);
        if (total == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(approvedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
