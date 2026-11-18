package com.plasticene.boot.flow.core.impl.application;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.param.FlowCategoryParam;
import com.plasticene.boot.flow.core.api.query.FlowCategoryQuery;
import com.plasticene.boot.flow.core.api.service.FlowCategoryApi;
import com.plasticene.boot.flow.core.api.spi.FlowAction;
import com.plasticene.boot.flow.core.api.validation.group.Insert;
import com.plasticene.boot.flow.core.api.validation.group.Update;
import com.plasticene.boot.flow.core.api.vo.FlowCategoryVO;
import com.plasticene.boot.flow.core.impl.service.category.FlowCategoryService;
import com.plasticene.boot.flow.core.impl.support.FlowActors;
import jakarta.annotation.Resource;
import jakarta.validation.groups.Default;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 实现 {@link FlowCategoryApi}，负责流程分类管理的授权与用例编排。
 * @author ZFJ
 * @since 2026-11-16
 */
@Service
public class FlowCategoryApiImpl implements FlowCategoryApi {
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
    public void update(FlowCategoryParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Update.class);
        access.check(actor, FlowAction.CATEGORY_UPDATE, access.category(param.getId()));
        categoryService.updateCategory(param, actor);
    }

    @Override
    public void delete(List<Long> ids, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.CATEGORY_DELETE, access.organization(actor));
        if (ids == null || ids.isEmpty()) {
            throw new BizException("ID列表不能为空");
        }
        for (Long id : ids) {
            access.check(actor, FlowAction.CATEGORY_DELETE, access.category(id));
        }
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
    public void sort(List<Long> ids, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.CATEGORY_SORT, access.organization(actor));
        if (ids == null || ids.isEmpty()) {
            throw new BizException("ID列表不能为空");
        }
        for (Long id : ids) {
            access.check(actor, FlowAction.CATEGORY_SORT, access.category(id));
        }
        categoryService.sortCategory(ids, actor);
    }
}
