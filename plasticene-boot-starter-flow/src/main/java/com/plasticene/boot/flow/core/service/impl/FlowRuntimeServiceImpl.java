package com.plasticene.boot.flow.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.RequestUserHolder;
import com.plasticene.boot.flow.core.dao.FlowInstanceDAO;
import com.plasticene.boot.flow.core.dto.ProcessNode;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowProcess;
import com.plasticene.boot.flow.core.enums.FlowInstanceEventTypeEnum;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowProcessStatusEnum;
import com.plasticene.boot.flow.core.event.InstanceEvent;
import com.plasticene.boot.flow.core.executor.ProcessInstanceExecutor;
import com.plasticene.boot.flow.core.parser.FlowParser;
import com.plasticene.boot.flow.core.service.FlowProcessService;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private FlowProcessService flowProcessService;
    @Resource
    private ProcessInstanceExecutor processInstanceExecutor;
    @Resource
    private ApplicationContext applicationContext;


    @Override
    public long startFlowInstanceById(Long processId, Long businessId, Map<String, Object> varMap) {
        FlowProcess process = flowProcessService.getById(processId);
        if (process == null) {
            throw new BizException("流程模型不存在");
        }
        Integer status = process.getStatus();
        if (!Objects.equals(status, FlowProcessStatusEnum.RELEASE.getCode())) {
            throw new BizException("当前类型不是发布状态");
        }
        // 发布状态的流程是合法的，可以直接使用，这里不需要再校验合法性
        ProcessNode processNode = process.getProcessNode();
        FlowInstance instance = new FlowInstance();
        instance.setProcessId(processId);
        instance.setBusinessId(businessId);
        instance.setVarMap(varMap);
        instance.setCategory(process.getCategory());
        instance.setOrgId(process.getOrgId());
        instance.setStartTime(LocalDateTime.now());
        instance.setCurrentNodeKey(processNode.getKey());
        instance.setCurrentNodeName(processNode.getName());
        LoginUser loginUser = RequestUserHolder.getLoginUser();
        instance.setUserId(loginUser.getId());
        flowInstanceDAO.insert(instance);

        // 开始流转流程
        FlowParser.makeParentNode(processNode);
        processInstanceExecutor.executeNode(instance, processNode);

        // 发布流程实例开始事件
        InstanceEvent instanceEvent = buildInstanceEvent(instance, processNode, FlowInstanceEventTypeEnum.START);
        applicationContext.publishEvent(instanceEvent);
        return instance.getId();
    }

    @Override
    public FlowInstance selectInstanceForUpdate(Long instanceId) {
        return flowInstanceDAO.selectInstanceForUpdate(instanceId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateInstanceCurrentNode(Long instanceId, ProcessNode currentNode) {
        FlowInstance instance = new FlowInstance();
        instance.setId(instanceId);
        instance.setCurrentNodeKey(currentNode.getKey());
        instance.setCurrentNodeName(currentNode.getName());
        flowInstanceDAO.updateById(instance);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void endInstance(FlowInstance instance, ProcessNode currentNode, FlowInstanceStatusEnum status) {
        FlowInstance updateInstance = new FlowInstance();
        updateInstance.setId(instance.getId());
        updateInstance.setStatus(status.getCode());
        updateInstance.setEndTime(LocalDateTime.now());
        flowInstanceDAO.updateById(updateInstance);

        // 发布流程结束事件
        InstanceEvent instanceEvent = buildInstanceEvent(instance, currentNode, FlowInstanceEventTypeEnum.END);
        applicationContext.publishEvent(instanceEvent);

    }

    private InstanceEvent buildInstanceEvent(FlowInstance instance, ProcessNode currentNode,
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
