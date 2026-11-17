package com.plasticene.boot.flow.core.impl.application;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.param.FlowBusinessStartByCodeParam;
import com.plasticene.boot.flow.core.api.param.FlowBusinessStartByDefinitionParam;
import com.plasticene.boot.flow.core.api.param.FlowFormStartParam;
import com.plasticene.boot.flow.core.api.param.FlowRoutePreviewParam;
import com.plasticene.boot.flow.core.api.service.FlowInstanceApi;
import com.plasticene.boot.flow.core.api.spi.FlowAction;
import com.plasticene.boot.flow.core.api.vo.FlowRouteNodeVO;
import com.plasticene.boot.flow.core.impl.service.instance.FlowRuntimeService;
import com.plasticene.boot.flow.core.impl.support.FlowActors;
import jakarta.annotation.Resource;
import jakarta.validation.groups.Default;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 实现 {@link FlowInstanceApi}，负责申请发起与取消的授权与用例编排。
 * @author ZFJ
 * @since 2026-11-16
 */
@Service
public class FlowInstanceApiImpl implements FlowInstanceApi {
    @Resource
    private FlowAccess access;
    @Resource
    private FlowRuntimeService runtimeService;

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long startBusinessByCode(FlowBusinessStartByCodeParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Default.class);
        access.requireBusinessId(param.getBusinessId());
        Long definitionId = access.activeDefinition(param.getProcessCode(), actor);
        access.check(actor, FlowAction.INSTANCE_START, access.definition(definitionId));
        return runtimeService.startInstance(definitionId, param.getBusinessId(), param.getVarMap(), actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long startBusinessByDefinition(FlowBusinessStartByDefinitionParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Default.class);
        access.check(actor, FlowAction.INSTANCE_START, access.definition(param.getDefinitionId()));
        access.requireBusinessId(param.getBusinessId());
        return runtimeService.startInstance(param.getDefinitionId(), param.getBusinessId(), param.getVarMap(), actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long startForm(FlowFormStartParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Default.class);
        access.check(actor, FlowAction.INSTANCE_START, access.definition(param.getDefinitionId()));
        return runtimeService.startInstance(param.getDefinitionId(), param.getVarMap(), actor);
    }

    @Override
    public List<FlowRouteNodeVO> previewRoute(FlowRoutePreviewParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Default.class);
        access.check(actor, FlowAction.INSTANCE_START, access.definition(param.getDefinitionId()));
        return runtimeService.calculateRoute(param.getDefinitionId(), param.getVarMap(), actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void cancel(Long id, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.INSTANCE_CANCEL, access.instance(id));
        runtimeService.cancelInstance(id, actor);
    }
}
