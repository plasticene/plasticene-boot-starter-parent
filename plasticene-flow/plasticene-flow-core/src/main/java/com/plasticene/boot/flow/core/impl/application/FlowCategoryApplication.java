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
import com.plasticene.boot.flow.core.impl.service.category.FlowCategoryService;

/**
 * 实现FlowCategoryOperations的统一授权与操作编排。
 * @author ZFJ
 * @since 2026-11-16
 */
@Service
public class FlowCategoryApplication implements FlowCategoryOperations {
    @Resource
    private FlowAccess access;
    @Resource
    private FlowCategoryService categoryService;

    @Override
    public Long create(FlowCategoryParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Insert.class);
        access.check(actor, FlowAction.CATEGORY_CREATE, access.organization(actor));
        return categoryService.createCategory(param, actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(FlowCategoryParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Update.class);
        access.check(actor, FlowAction.CATEGORY_UPDATE, access.category(param.getId()));
        categoryService.updateCategory(param, actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.CATEGORY_DELETE, access.organization(actor));
        if (ids == null || ids.isEmpty()) { throw new com.plasticene.boot.common.exception.BizException("ID列表不能为空"); }
        for (Long id : ids) { access.check(actor, FlowAction.CATEGORY_DELETE, access.category(id)); }
        categoryService.delete(ids, actor);
    }

    @Override
    public List<FlowCategoryVO> list(LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.CATEGORY_READ, access.organization(actor));
        return categoryService.listCategory(actor);
    }

    @Override
    public PageResult<FlowCategoryVO> page(FlowCategoryQuery query, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(query, Default.class);
        access.check(actor, FlowAction.CATEGORY_READ, access.organization(actor));
        return categoryService.page(query, actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sort(List<Long> ids, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.CATEGORY_SORT, access.organization(actor));
        if (ids == null || ids.isEmpty()) { throw new com.plasticene.boot.common.exception.BizException("ID列表不能为空"); }
        for (Long id : ids) { access.check(actor, FlowAction.CATEGORY_SORT, access.category(id)); }
        categoryService.sortCategory(ids, actor);
    }
}
