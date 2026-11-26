package com.plasticene.boot.flow.business.fields.application;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.business.fields.persistence.dao.FlowBusinessFieldsQueryDAO;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsCategoryService;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsQuerySupport;
import com.plasticene.boot.flow.core.api.query.FlowInstanceQuery;
import com.plasticene.boot.flow.core.api.query.FlowTaskQuery;
import com.plasticene.boot.flow.core.api.service.FlowBusinessFieldsApi;
import com.plasticene.boot.flow.core.api.spi.FlowAction;
import com.plasticene.boot.flow.core.api.vo.FlowBusinessFieldsVO;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceVO;
import com.plasticene.boot.flow.core.api.vo.FlowTaskPageVO;
import com.plasticene.boot.flow.core.impl.application.FlowAccess;
import com.plasticene.boot.mybatis.core.utils.MybatisUtils;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 提供审批业务字段配置和个人分页查询。
 * @author ZFJ
 * @since 2026-11-20
 */
@RequiredArgsConstructor
public class FlowBusinessFieldsApiImpl implements FlowBusinessFieldsApi {

    private final FlowAccess access;
    private final FlowBusinessFieldsCategoryService categories;
    private final FlowBusinessFieldsQuerySupport querySupport;
    private final FlowBusinessFieldsQueryDAO queries;

    @Override
    public FlowBusinessFieldsVO.Limits limits() {
        return categories.limits();
    }

    @Override
    public List<FlowBusinessFieldsVO.Category> enrichCatalog(List<FlowBusinessFieldsVO.Category> catalog, LoginUser actor) {
        return categories.enrich(catalog, actor.getOrgId());
    }

    @Override
    public FlowBusinessFieldsVO.Configuration getFields(Long categoryId, LoginUser actor) {
        access.check(actor, FlowAction.CATEGORY_READ, access.category(categoryId));
        return categories.get(actor.getOrgId(), categoryId);
    }

    @Override
    public void saveFields(Long categoryId, List<FlowBusinessFieldsVO.Field> fields, LoginUser actor) {
        access.check(actor, FlowAction.CATEGORY_UPDATE, access.category(categoryId));
        categories.save(categoryId, fields, actor);
    }

    @Override
    public IPage<FlowInstanceVO> pageInstances(FlowInstanceQuery scopedQuery) {
        var filters = querySupport.compile(scopedQuery.getOrgId(), scopedQuery.getCategory(), scopedQuery.getBusinessFilters());
        IPage<FlowInstanceVO> page = queries.pageInstance(MybatisUtils.buildPage(scopedQuery), scopedQuery, true, filters);
        querySupport.enrich(scopedQuery.getOrgId(), scopedQuery.getCategory(), page.getRecords(),
                FlowInstanceVO::getId, FlowInstanceVO::getDefinitionId, FlowInstanceVO::setListValues);
        return page;
    }

    @Override
    public IPage<FlowTaskPageVO> pageTasks(FlowTaskQuery scopedQuery) {
        var filters = querySupport.compile(scopedQuery.getOrgId(), scopedQuery.getCategory(), scopedQuery.getBusinessFilters());
        IPage<FlowTaskPageVO> page = queries.pageTask(MybatisUtils.buildPage(scopedQuery), scopedQuery, filters);
        querySupport.enrich(scopedQuery.getOrgId(), scopedQuery.getCategory(), page.getRecords(),
                FlowTaskPageVO::getInstanceId, FlowTaskPageVO::getDefinitionId, FlowTaskPageVO::setListValues);
        return page;
    }
}
