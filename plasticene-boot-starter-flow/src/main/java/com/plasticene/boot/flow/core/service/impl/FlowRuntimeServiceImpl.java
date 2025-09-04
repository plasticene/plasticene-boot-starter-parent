package com.plasticene.boot.flow.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.RequestUserHolder;
import com.plasticene.boot.flow.core.dao.FlowInstanceDAO;
import com.plasticene.boot.flow.core.dto.ProcessNode;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowProcess;
import com.plasticene.boot.flow.core.executor.ProcessInstanceExecutor;
import com.plasticene.boot.flow.core.parser.FlowParser;
import com.plasticene.boot.flow.core.service.FlowProcessService;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

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





    @Transactional(rollbackFor = Exception.class)
    @Override
    public FlowInstance startFlowInstanceById(Long processId) {
        FlowProcess process = flowProcessService.getById(processId);
        if (process == null) {
            throw new BizException("流程模型不存在");
        }
        ProcessNode processNode = FlowParser.parseProcessNode(process.getModel());
        boolean isValid = FlowParser.validateProcessNode(processNode);
        if (!isValid) {
            throw new BizException("当前流程模型不合法");
        }
        FlowInstance instance = new FlowInstance();
        instance.setProcessId(processId);
        instance.setModel(process.getModel());
        instance.setOrgId(process.getOrgId());
        instance.setStartTime(new Date());
        instance.setCurrentNodeKey(processNode.getKey());
        instance.setCurrentNodeName(processNode.getName());
        // todo 处理表单 申请人等
        instance.setUserId(1L);
        flowInstanceDAO.insert(instance);

        // 开始流转流程
        processInstanceExecutor.executeNode(instance, processNode);

        return instance;
    }
}
