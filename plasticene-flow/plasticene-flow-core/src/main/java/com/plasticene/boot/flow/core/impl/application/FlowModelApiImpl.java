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
import com.plasticene.boot.flow.core.impl.service.model.FlowModelService;
import com.plasticene.boot.flow.core.impl.service.definition.FlowDefinitionService;

/**
 * 实现FlowModelOperations的统一授权与操作编排。
 * @author ZFJ
 * @since 2026-11-16
 */
@Service
public class FlowModelApiImpl implements FlowModelApi {
    @Resource
    private FlowAccess access;
    @Resource
    private FlowModelService modelService;
    @Resource
    private FlowDefinitionService definitionService;
    @Resource
    private FlowVariables variables;

    @Override
    public Long create(FlowModelParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Insert.class);
        access.check(actor, FlowAction.MODEL_CREATE, access.organization(actor));
        return modelService.create(param, actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(FlowModelParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Update.class);
        access.check(actor, FlowAction.MODEL_UPDATE, access.model(param.getId()));
        modelService.update(param, actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.MODEL_DELETE, access.organization(actor));
        if (ids == null || ids.isEmpty()) { throw new com.plasticene.boot.common.exception.BizException("ID列表不能为空"); }
        for (Long id : ids) { access.check(actor, FlowAction.MODEL_DELETE, access.model(id)); }
        modelService.delete(ids, actor);
    }

    @Override
    public FlowModelVO get(Long id, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.MODEL_READ, access.model(id));
        return modelService.detail(id, actor);
    }

    @Override
    public PageResult<FlowModelVO> page(FlowModelQuery query, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(query, Default.class);
        access.check(actor, FlowAction.MODEL_READ, access.organization(actor));
        return modelService.page(query, actor);
    }

    @Override
    public FlowModelStatisticsVO statistics(FlowModelQuery query, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(query, Default.class);
        access.check(actor, FlowAction.MODEL_READ, access.organization(actor));
        return modelService.statistics(query, actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setEnabled(FlowModelEnableParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Default.class);
        access.check(actor, FlowAction.MODEL_ENABLE, access.model(param.getId()));
        modelService.enable(param, actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.MODEL_PUBLISH, access.model(id));
        modelService.publish(id, actor);
    }

    @Override
    public List<FlowDefinitionVO> listVersions(Long id, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.MODEL_READ, access.model(id));
        return definitionService.listModelVersions(id, actor);
    }

    @Override
    public FlowDefinitionVO getVersion(Long id, Long definitionId, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.MODEL_READ, access.model(id));
        return definitionService.getModelVersion(id, definitionId, actor);
    }

    @Override
    public FlowVariableSchema getVariableSchema(Long id, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.MODEL_READ, access.model(id));
        return variables.forModel(access.ownedModel(id, actor));
    }
}
