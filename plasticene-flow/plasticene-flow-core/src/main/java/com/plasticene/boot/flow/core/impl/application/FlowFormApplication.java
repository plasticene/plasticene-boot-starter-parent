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
import com.plasticene.boot.flow.core.impl.service.form.FlowFormService;

/**
 * 实现FlowFormOperations的统一授权与操作编排。
 * @author ZFJ
 * @since 2026-11-16
 */
@Service
public class FlowFormApplication implements FlowFormOperations {
    @Resource
    private FlowAccess access;
    @Resource
    private FlowFormService formService;

    @Override
    public Long create(FlowFormParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Insert.class);
        access.check(actor, FlowAction.FORM_CREATE, access.organization(actor));
        return formService.create(param, actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(FlowFormParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Update.class);
        access.check(actor, FlowAction.FORM_UPDATE, access.form(param.getId()));
        formService.update(param, actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.FORM_DELETE, access.organization(actor));
        if (ids == null || ids.isEmpty()) { throw new com.plasticene.boot.common.exception.BizException("ID列表不能为空"); }
        for (Long id : ids) { access.check(actor, FlowAction.FORM_DELETE, access.form(id)); }
        formService.delete(ids, actor);
    }

    @Override
    public List<FlowFormVO> list(LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.FORM_READ, access.organization(actor));
        return formService.listAll(actor);
    }

    @Override
    public PageResult<FlowFormVO> page(FlowFormQuery query, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(query, Default.class);
        access.check(actor, FlowAction.FORM_READ, access.organization(actor));
        return formService.page(query, actor);
    }

    @Override
    public FlowFormVO get(Long id, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.FORM_READ, access.form(id));
        return formService.detail(id, actor);
    }
}
