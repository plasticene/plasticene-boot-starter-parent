package com.plasticene.boot.flow.core.impl.application;

import org.springframework.beans.factory.annotation.Autowired;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.query.FlowDashboardQuery;
import com.plasticene.boot.flow.core.api.query.FlowInstanceQuery;
import com.plasticene.boot.flow.core.api.query.FlowTaskQuery;
import com.plasticene.boot.flow.core.api.service.FlowQueryApi;
import com.plasticene.boot.flow.core.api.spi.FlowAction;
import com.plasticene.boot.flow.core.api.vo.FlowDashboardVO;
import com.plasticene.boot.flow.core.api.vo.FlowDefinitionVO;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceDetailVO;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceStatisticsVO;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceVO;
import com.plasticene.boot.flow.core.api.vo.FlowStartCatalogVO;
import com.plasticene.boot.flow.core.api.vo.FlowTaskPageVO;
import com.plasticene.boot.flow.core.impl.service.dashboard.FlowDashboardService;
import com.plasticene.boot.flow.core.impl.service.definition.FlowDefinitionService;
import com.plasticene.boot.flow.core.impl.service.instance.FlowRuntimeService;
import com.plasticene.boot.flow.core.impl.service.task.FlowTaskService;
import com.plasticene.boot.flow.core.impl.support.FlowActors;
import jakarta.annotation.Resource;
import com.plasticene.boot.flow.core.api.vo.FlowBusinessFieldsVO;
import com.plasticene.boot.flow.core.api.vo.FlowCategoryVO;
import com.plasticene.boot.flow.core.impl.service.category.FlowCategoryService;
import java.util.Comparator;
import java.util.List;
import jakarta.validation.groups.Default;
import org.springframework.stereotype.Service;

/**
 * 实现 {@link FlowQueryApi}，负责流程数据查询的授权与用例编排。
 * @author ZFJ
 * @since 2026-11-16
 */
@Service
public class FlowQueryApiImpl implements FlowQueryApi {
    @Autowired(required = false)
    private FlowBusinessFieldsSupportProvider businessFields;

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

    @Resource
    private FlowCategoryService categoryService;

    @Override
    public FlowBusinessFieldsVO.Catalog getApprovalCatalog(LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.INSTANCE_READ, access.organization(actor));
        var categories = categoryService.listCategory(actor).stream()
                .sorted(Comparator.comparing(FlowCategoryVO::getSeq, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(FlowCategoryVO::getId, Comparator.reverseOrder()))
                .map(category -> new FlowBusinessFieldsVO.Category(category.getId(), category.getCode(),
                        category.getName(), category.getIcon(), category.getSeq(), List.of()))
                .toList();
        var extension = businessFields == null ? null : businessFields.api();
        return extension == null ? new FlowBusinessFieldsVO.Catalog(false, null, categories)
                : new FlowBusinessFieldsVO.Catalog(true, extension.limits(), extension.enrichCatalog(categories, actor));
    }

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
