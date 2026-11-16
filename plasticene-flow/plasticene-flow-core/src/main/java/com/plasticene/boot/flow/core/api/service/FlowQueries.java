package com.plasticene.boot.flow.core.api.service;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.flow.core.api.param.*;
import com.plasticene.boot.flow.core.api.query.*;
import com.plasticene.boot.flow.core.api.vo.*;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import java.util.List;

/**
 * 公开FlowQueries完整用例。
 * @author ZFJ
 * @since 2026-11-16
 */
public interface FlowQueries {
    PageResult<FlowInstanceVO> pageMyInstances(FlowInstanceQuery query, LoginUser actor);
    PageResult<FlowInstanceVO> pageInstances(FlowInstanceQuery query, LoginUser actor);
    FlowInstanceStatisticsVO instanceStatistics(FlowInstanceQuery query, LoginUser actor);
    FlowDashboardVO dashboard(FlowDashboardQuery query, LoginUser actor);
    FlowInstanceDetailVO getInstance(Long id, Long taskId, LoginUser actor);
    FlowStartCatalogVO getStartableCatalog(LoginUser actor);
    FlowDefinitionVO getStartableDefinition(Long id, LoginUser actor);
    PageResult<FlowTaskPageVO> pageMyTodo(FlowTaskQuery query, LoginUser actor);
    PageResult<FlowTaskPageVO> pageMyDone(FlowTaskQuery query, LoginUser actor);
    PageResult<FlowTaskPageVO> pageMyCopy(FlowTaskQuery query, LoginUser actor);
}
