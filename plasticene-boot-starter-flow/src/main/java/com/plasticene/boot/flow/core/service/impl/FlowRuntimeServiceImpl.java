package com.plasticene.boot.flow.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.dao.FlowInstanceDAO;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.enums.FlowInstanceEventTypeEnum;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowProcessStatusEnum;
import com.plasticene.boot.flow.core.event.InstanceEvent;
import com.plasticene.boot.flow.core.parser.FlowParser;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * @author ZFJ
 * @date 2025/9/4
 */
@Service
public class FlowRuntimeServiceImpl extends ServiceImpl<FlowInstanceDAO, FlowInstance> implements FlowRuntimeService {
    @Resource
    private FlowInstanceDAO flowInstanceDAO;
    @Resource
    @Lazy
    private ProcessExecutor processExecutor;
    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private FlowDefinitionService flowDefinitionService;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public long startFlowInstanceById(Long definitionId, Long businessId, Map<String, Object> varMap) {
        FlowDefinition flowDefinition = flowDefinitionService.getById(definitionId);
        Assert.notNull(flowDefinition, "当前发布流程模型不存在");
        // 发布了的流程模型是合法的，可以直接使用，这里不需要再校验合法性，发布的时候已经校验过合法性了
        FlowNode flowNode = flowDefinition.getModelNode();
        FlowInstance instance = new FlowInstance();
        instance.setDefinitionId(definitionId);
        instance.setBusinessId(businessId);
        instance.setVarMap(varMap);
        instance.setCategory(flowDefinition.getCategory());
        instance.setOrgId(flowDefinition.getOrgId());
        instance.setStartTime(LocalDateTime.now());
        instance.setStatus(FlowInstanceStatusEnum.RUNNING.getCode());
        instance.setCurrentNodeKey(flowNode.getKey());
        instance.setCurrentNodeName(flowNode.getName());
        LoginUser loginUser = LoginUserHolder.get();
        instance.setUserId(loginUser.getId());
        flowInstanceDAO.insert(instance);

        // 发布流程实例开始事件
        InstanceEvent instanceEvent = buildInstanceEvent(instance, flowNode, FlowInstanceEventTypeEnum.START);
        applicationContext.publishEvent(instanceEvent);

        // 开始流转流程
        FlowParser.makeParentNode(flowNode);
        processExecutor.executeNode(instance, flowNode);

        return instance.getId();
    }

    @Override
    public FlowInstance selectInstanceForUpdate(Long instanceId) {
        return flowInstanceDAO.selectInstanceForUpdate(instanceId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateInstanceCurrentNode(Long instanceId, FlowNode currentNode) {
        FlowInstance instance = new FlowInstance();
        instance.setId(instanceId);
        instance.setCurrentNodeKey(currentNode.getKey());
        instance.setCurrentNodeName(currentNode.getName());
        flowInstanceDAO.updateById(instance);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void endInstance(FlowInstance instance, FlowNode currentNode, FlowInstanceStatusEnum status) {
        // 更新按照规范只更新必须的字段
        FlowInstance updateInstance = new FlowInstance();
        updateInstance.setId(instance.getId());
        updateInstance.setStatus(status.getCode());
        updateInstance.setEndTime(LocalDateTime.now());
        updateInstance.setCurrentNodeKey(currentNode.getKey());
        updateInstance.setCurrentNodeName(currentNode.getName());
        flowInstanceDAO.updateById(updateInstance);
        // 上下文instance需要传递最新状态
        instance.setStatus(status.getCode());
        // 发布流程结束事件
        InstanceEvent instanceEvent = buildInstanceEvent(instance, currentNode, FlowInstanceEventTypeEnum.END);
        applicationContext.publishEvent(instanceEvent);

    }

    private InstanceEvent buildInstanceEvent(FlowInstance instance, FlowNode currentNode,
                                             FlowInstanceEventTypeEnum eventType) {
        InstanceEvent instanceEvent = new InstanceEvent(this);
        instanceEvent.setInstance(instance);
        instanceEvent.setCurrentNode(currentNode);
        instanceEvent.setBusinessId(instance.getBusinessId());
        instanceEvent.setCategory(instance.getCategory());
        instanceEvent.setEventType(eventType);
        return instanceEvent;
    }
}
