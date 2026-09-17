package com.plasticene.boot.flow.core.service.impl;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.dao.FlowDashboardDAO;
import com.plasticene.boot.flow.core.enums.FlowDashboardGranularityEnum;
import com.plasticene.boot.flow.core.model.query.FlowDashboardQuery;
import com.plasticene.boot.flow.core.model.vo.FlowDashboardVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 流程数据总览服务测试
 *
 * @author ZFJ
 * @since 2026-09-17
 */
@ExtendWith(MockitoExtension.class)
class FlowDashboardServiceImplTest {

    @Mock
    private FlowDashboardDAO flowDashboardDAO;

    @InjectMocks
    private FlowDashboardServiceImpl flowDashboardService;

    private FlowDashboardQuery query;

    @BeforeEach
    void setUp() {
        query = new FlowDashboardQuery();
        query.setOrgId(1L);
        query.setStartTime(LocalDateTime.of(2026, 9, 1, 0, 0));
        query.setEndTime(LocalDateTime.of(2026, 9, 4, 0, 0));
        query.setGranularity(FlowDashboardGranularityEnum.DAY);
    }

    @Test
    void shouldCalculateRatesAndFillMissingTrendBuckets() {
        FlowDashboardVO.Summary summary = new FlowDashboardVO.Summary();
        summary.setApproved(8L);
        summary.setRejected(2L);
        FlowDashboardVO.Summary currentDuration = new FlowDashboardVO.Summary();
        currentDuration.setMedianDurationSeconds(3600L);
        currentDuration.setP90DurationSeconds(7200L);
        FlowDashboardVO.Summary previousDuration = new FlowDashboardVO.Summary();
        previousDuration.setMedianDurationSeconds(5400L);

        FlowDashboardVO.TrendItem first = trend(LocalDate.of(2026, 9, 1), 3L, 2L);
        FlowDashboardVO.TrendItem last = trend(LocalDate.of(2026, 9, 3), 5L, 4L);
        FlowDashboardVO.ModelRanking ranking = new FlowDashboardVO.ModelRanking();
        ranking.setApproved(3L);
        ranking.setRejected(1L);

        when(flowDashboardDAO.selectSummary(any(), any(), any())).thenReturn(summary);
        when(flowDashboardDAO.selectDurationStatistics(anyLong(), any(), any()))
                .thenReturn(currentDuration, previousDuration);
        when(flowDashboardDAO.listTrends(query)).thenReturn(List.of(first, last));
        when(flowDashboardDAO.selectBacklog(anyLong(), any(), any())).thenReturn(new FlowDashboardVO.Backlog());
        when(flowDashboardDAO.listModelRankings(any(), any())).thenReturn(new ArrayList<>(List.of(ranking)));
        when(flowDashboardDAO.listNodeRankings(any(), any())).thenReturn(List.of());

        FlowDashboardVO result = flowDashboardService.overview(query);

        assertEquals(new BigDecimal("80.0"), result.getSummary().getApprovalRate());
        assertEquals(3600L, result.getSummary().getMedianDurationSeconds());
        assertEquals(5400L, result.getSummary().getPreviousMedianDurationSeconds());
        assertEquals(3, result.getTrends().size());
        assertEquals(0L, result.getTrends().get(1).getStarted());
        assertEquals(new BigDecimal("75.0"), result.getModelRankings().getFirst().getApprovalRate());
    }

    @Test
    void shouldRejectRangesLongerThanOneYear() {
        query.setEndTime(query.getStartTime().plusDays(367));

        assertThrows(BizException.class, () -> flowDashboardService.overview(query));
    }

    private FlowDashboardVO.TrendItem trend(LocalDate date, Long started, Long completed) {
        FlowDashboardVO.TrendItem item = new FlowDashboardVO.TrendItem();
        item.setDate(date);
        item.setStarted(started);
        item.setCompleted(completed);
        return item;
    }
}
