package com.plasticene.boot.flow.core.impl.application;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.param.FlowStartByCodeParam;
import com.plasticene.boot.flow.core.api.param.FlowStartByIdParam;
import com.plasticene.boot.flow.core.api.param.FlowStartParam;
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
    public Long startInstanceByCode(FlowStartByCodeParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Default.class);
        access.requireBusinessId(param.getBusinessId());
        Long definitionId = access.activeDefinition(param.getProcessCode(), actor);
        access.check(actor, FlowAction.INSTANCE_START, access.definition(definitionId));
        return runtimeService.startInstance(definitionId, param.getBusinessId(), param.getVarMap(), actor);
    }

    @Override
    public Long startInstanceById(FlowStartByIdParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Default.class);
        access.check(actor, FlowAction.INSTANCE_START, access.definition(param.getDefinitionId()));
        access.requireBusinessId(param.getBusinessId());
        return runtimeService.startInstance(param.getDefinitionId(), param.getBusinessId(), param.getVarMap(), actor);
    }

    @Override
    public Long startInstance(FlowStartParam param, LoginUser actor) {
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
    public void cancel(Long id, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.INSTANCE_CANCEL, access.instance(id));
        runtimeService.cancelInstance(id, actor);
    }
}
