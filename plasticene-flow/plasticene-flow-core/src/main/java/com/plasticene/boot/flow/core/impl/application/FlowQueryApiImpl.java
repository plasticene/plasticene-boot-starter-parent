package com.plasticene.boot.flow.core.impl.application;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.impl.support.FlowActors;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.flow.core.api.param.*;
import com.plasticene.boot.flow.core.api.query.*;
import com.plasticene.boot.flow.core.api.vo.*;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import java.util.List;
import com.plasticene.boot.flow.core.api.service.*;
import com.plasticene.boot.flow.core.api.spi.FlowAction;
import com.plasticene.boot.flow.core.api.validation.group.*;
import jakarta.validation.groups.Default;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.plasticene.boot.flow.core.impl.service.definition.FlowDefinitionService;
import com.plasticene.boot.flow.core.impl.service.instance.FlowRuntimeService;
import com.plasticene.boot.flow.core.impl.service.task.FlowTaskService;
import com.plasticene.boot.flow.core.impl.service.dashboard.FlowDashboardService;

/**
 * 实现FlowQueries的统一授权与操作编排。
 * @author ZFJ
 * @since 2026-11-16
 */
@Service
public class FlowQueryApiImpl implements FlowQueryApi {
    @Resource
    private FlowAccess access;
    @Resource
    private FlowDefinitionService definitionService;
    @Resource
    private FlowRuntimeService runtimeService;
    @Resource
    private FlowTaskService taskService;
    @Resource
    private FlowDashboardService dashboardService;

    @Override
    public PageResult<FlowInstanceVO> pageMyInstances(FlowInstanceQuery query, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.INSTANCE_READ, access.organization(actor));
        return runtimeService.page(access.scopeInstances(query, actor, true), actor);
    }

    @Override
    public PageResult<FlowInstanceVO> pageInstances(FlowInstanceQuery query, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.INSTANCE_READ_ORG, access.organization(actor));
        return runtimeService.page(access.scopeInstances(query, actor, false), actor);
    }

    @Override
    public FlowInstanceStatisticsVO instanceStatistics(FlowInstanceQuery query, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.INSTANCE_READ_ORG, access.organization(actor));
        return runtimeService.statistics(access.scopeInstances(query, actor, false));
    }

    @Override
    public FlowDashboardVO dashboard(FlowDashboardQuery query, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.DASHBOARD_READ, access.organization(actor));
        return dashboardService.overview(access.scopeDashboard(query, actor));
    }

    @Override
    public FlowInstanceDetailVO getInstance(Long id, Long taskId, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.INSTANCE_READ, access.instance(id));
        access.requireInstanceReader(id, actor);
        return runtimeService.getInstanceDetail(id, taskId, actor);
    }

    @Override
    public FlowStartCatalogVO getStartableCatalog(LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.INSTANCE_START, access.organization(actor));
        return definitionService.getStartableCatalog(actor);
    }

    @Override
    public FlowDefinitionVO getStartableDefinition(Long id, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.INSTANCE_START, access.definition(id));
        return definitionService.getFlowDefinition(id, actor);
    }

    @Override
    public PageResult<FlowTaskPageVO> pageMyTodo(FlowTaskQuery query, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(query, Default.class);
        access.check(actor, FlowAction.INSTANCE_READ, access.organization(actor));
        return taskService.pageMyTodo(query, actor);
    }

    @Override
    public PageResult<FlowTaskPageVO> pageMyDone(FlowTaskQuery query, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(query, Default.class);
        access.check(actor, FlowAction.INSTANCE_READ, access.organization(actor));
        return taskService.pageMyDone(query, actor);
    }

    @Override
    public PageResult<FlowTaskPageVO> pageMyCopy(FlowTaskQuery query, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(query, Default.class);
        access.check(actor, FlowAction.INSTANCE_READ, access.organization(actor));
        return taskService.pageMyCopy(query, actor);
    }
}
