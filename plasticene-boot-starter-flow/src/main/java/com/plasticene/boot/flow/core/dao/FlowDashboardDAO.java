package com.plasticene.boot.flow.core.dao;

import com.plasticene.boot.flow.core.model.query.FlowDashboardQuery;
import com.plasticene.boot.flow.core.model.vo.FlowDashboardVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程数据总览数据访问层
 *
 * @author ZFJ
 * @since 2026-09-17
 */
@Mapper
public interface FlowDashboardDAO {

    FlowDashboardVO.Summary selectSummary(@Param("query") FlowDashboardQuery query,
                                          @Param("previousStartTime") LocalDateTime previousStartTime,
                                          @Param("longRunningBefore") LocalDateTime longRunningBefore);

    FlowDashboardVO.Summary selectDurationStatistics(@Param("orgId") Long orgId,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    List<FlowDashboardVO.TrendItem> listTrends(@Param("query") FlowDashboardQuery query);

    FlowDashboardVO.Backlog selectBacklog(@Param("orgId") Long orgId,
                                          @Param("oneDayBefore") LocalDateTime oneDayBefore,
                                          @Param("threeDaysBefore") LocalDateTime threeDaysBefore);

    List<FlowDashboardVO.ModelRanking> listModelRankings(@Param("query") FlowDashboardQuery query,
                                                        @Param("longRunningBefore") LocalDateTime longRunningBefore);

    List<FlowDashboardVO.NodeRanking> listNodeRankings(@Param("query") FlowDashboardQuery query,
                                                      @Param("now") LocalDateTime now);
}
