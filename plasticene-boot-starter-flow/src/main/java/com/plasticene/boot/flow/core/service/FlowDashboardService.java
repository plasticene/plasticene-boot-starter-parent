package com.plasticene.boot.flow.core.service;

import com.plasticene.boot.flow.core.model.query.FlowDashboardQuery;
import com.plasticene.boot.flow.core.model.vo.FlowDashboardVO;

/**
 * 流程数据总览服务
 *
 * @author ZFJ
 * @since 2026-09-17
 */
public interface FlowDashboardService {

    /**
     * 查询当前组织的流程数据总览
     *
     * @param query 查询参数
     * @return 流程数据总览
     */
    FlowDashboardVO overview(FlowDashboardQuery query);
}
