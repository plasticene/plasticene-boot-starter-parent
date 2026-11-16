package com.plasticene.boot.flow.core.impl.application;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.spi.*;
import com.plasticene.boot.flow.core.api.query.*;
import com.plasticene.boot.flow.core.impl.persistence.dao.*;
import com.plasticene.boot.flow.core.impl.persistence.entity.*;
import com.plasticene.boot.flow.core.impl.support.FlowActors;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import java.util.Objects;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 执行公开用例的可信资源定位、宿主授权和参数校验。
 * @author ZFJ
 * @since 2026-11-16
 */
@Component
public class FlowAccess {
    @Resource private ObjectProvider<FlowAuthorizationPolicy> policies;
    @Resource private Validator validator;
    @Resource private FlowModelDAO models;
    @Resource private FlowDefinitionDAO definitions;
    @Resource private FlowInstanceDAO instances;
    @Resource private FlowTaskDAO tasks;
    @Resource private FlowFormDAO forms;
    @Resource private FlowCategoryDAO categories;

    public void check(LoginUser actor, FlowAction action, FlowResource resource) {
        FlowActors.require(actor);
        if (!Objects.equals(actor.getOrgId(), resource.orgId())) { throw new BizException("流程资源不存在"); }
        var policy = policies.getIfAvailable();
        if (policy == null || !policy.isAllowed(actor, action, resource)) { throw new BizException("无权执行流程操作：" + action); }
    }
    public void validate(Object param, Class<?> group) {
        if (param == null) { throw new BizException("参数不能为空"); }
        var violations = validator.validate(param, group, Default.class);
        if (!violations.isEmpty()) { throw new BizException(violations.stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().findFirst().orElseThrow()); }
    }
    public FlowResource organization(LoginUser actor) {
        FlowActors.require(actor);
        return new FlowResource(actor.getOrgId(), "ORGANIZATION", String.valueOf(actor.getOrgId()), null, null, null, null, null, null);
    }
    public FlowResource model(Long id) {
        var value = models.selectById(requireId(id));
        if (value == null) { throw new BizException("流程模型不存在"); }
        return new FlowResource(value.getOrgId(), "MODEL", String.valueOf(id), id, null, null, null, value.getCode(), value.getCategory());
    }
    public FlowModel ownedModel(Long id, LoginUser actor) {
        check(actor, FlowAction.MODEL_READ, model(id));
        return models.selectById(id);
    }
    public FlowResource definition(Long id) {
        var value = definitions.selectById(requireId(id));
        if (value == null) { throw new BizException("流程定义不存在"); }
        return new FlowResource(value.getOrgId(), "DEFINITION", String.valueOf(id), value.getModelId(), id, null, null, value.getCode(), value.getCategory());
    }
    public FlowResource instance(Long id) {
        var value = instances.selectById(requireId(id));
        if (value == null) { throw new BizException("流程实例不存在"); }
        var def = definitions.selectById(value.getDefinitionId());
        return new FlowResource(value.getOrgId(), "INSTANCE", String.valueOf(id), def == null ? null : def.getModelId(), value.getDefinitionId(), id, null, def == null ? null : def.getCode(), value.getCategory());
    }
    public void requireInstanceReader(Long id, LoginUser actor) {
        var value = instances.selectById(requireId(id));
        if (value == null || !Objects.equals(value.getOrgId(), actor.getOrgId())) {
            throw new BizException("流程实例不存在");
        }
        if (Objects.equals(value.getUserId(), actor.getId())) { return; }
        long participation = tasks.selectCount(new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getInstanceId, id).eq(FlowTask::getOrgId, actor.getOrgId())
                .eq(FlowTask::getAssignee, actor.getId()).eq(FlowTask::getIsDelete, 0));
        if (participation == 0) { check(actor, FlowAction.INSTANCE_READ_ORG, instance(id)); }
    }

    public FlowResource task(Long id) {
        var value = tasks.selectById(requireId(id));
        if (value == null) { throw new BizException("流程任务不存在"); }
        var instance = instance(value.getInstanceId());
        if (!Objects.equals(value.getOrgId(), instance.orgId())) { throw new BizException("任务组织不一致"); }
        return new FlowResource(instance.orgId(), "TASK", String.valueOf(id), instance.modelId(), instance.definitionId(), instance.instanceId(), id, instance.processCode(), instance.category());
    }
    public FlowResource form(Long id) {
        var value = forms.selectById(requireId(id));
        if (value == null) { throw new BizException("表单不存在"); }
        return new FlowResource(value.getOrgId(), "FORM", String.valueOf(id), null, null, null, null, null, value.getCategory());
    }
    public FlowResource category(Long id) {
        var value = categories.selectById(requireId(id));
        if (value == null) { throw new BizException("分类不存在"); }
        return new FlowResource(value.getOrgId(), "CATEGORY", String.valueOf(id), null, null, null, null, null, value.getCode());
    }
    public Long activeDefinition(String code, LoginUser actor) {
        FlowActors.require(actor);
        if (code == null || code.isBlank()) { throw new BizException("流程编码不能为空"); }
        var model = models.selectOne(new LambdaQueryWrapper<FlowModel>().eq(FlowModel::getOrgId, actor.getOrgId()).eq(FlowModel::getCode, code));
        if (model == null || model.getActiveDefinitionId() == null) { throw new BizException("流程不存在或尚未发布"); }
        check(actor, FlowAction.INSTANCE_START, definition(model.getActiveDefinitionId()));
        return model.getActiveDefinitionId();
    }
    public FlowInstanceQuery scopeInstances(FlowInstanceQuery query, LoginUser actor, boolean mine) {
        validate(query, Default.class);
        FlowInstanceQuery scoped = new FlowInstanceQuery();
        BeanUtils.copyProperties(query, scoped);
        scoped.setOrgId(actor.getOrgId());
        scoped.setUserId(mine ? actor.getId() : null);
        if (mine) { scoped.setStartUserId(actor.getId()); }
        return scoped;
    }
    public FlowDashboardQuery scopeDashboard(FlowDashboardQuery query, LoginUser actor) {
        validate(query, Default.class);
        FlowDashboardQuery scoped = new FlowDashboardQuery();
        BeanUtils.copyProperties(query, scoped);
        scoped.setOrgId(actor.getOrgId());
        return scoped;
    }
    public void requireBusinessId(String id) {
        if (id == null || id.isBlank() || id.length() > 100) { throw new BizException("申请记录ID必须为1至100字"); }
    }
    private Long requireId(Long id) {
        if (id == null || id <= 0) { throw new BizException("ID必须为正整数"); }
        return id;
    }
}
