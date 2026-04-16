package com.plasticene.boot.flow.core;

import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowProcess;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.param.FlowInstanceParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;
import com.plasticene.boot.flow.core.parser.FlowParser;
import com.plasticene.boot.flow.core.service.FlowProcessService;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * {@link FlowEngine} 默认实现，基于内部 Service/Executor 进行封装。
 *
 * <p>推荐业务方仅依赖本实现对外暴露的接口，避免直接耦合内部 Service，
 * 便于后续引擎实现的演进与扩展。</p>
 *
 * @author ZFJ
 * @date 2025/9/2
 */
@Service
public class FlowEngineImpl implements FlowEngine {

    @Resource
    private FlowRuntimeService flowRuntimeService;
    @Resource
    private FlowTaskService flowTaskService;
    @Resource
    private FlowProcessService flowProcessService;
    @Resource
    private ProcessExecutor processExecutor;

    @Override
    public Long startInstance(Long processId) {
        return flowRuntimeService.startFlowInstanceById(processId);
    }

    @Override
    public Long startInstance(Long processId, Long businessId) {
        return flowRuntimeService.startFlowInstanceById(processId, businessId);
    }

    @Override
    public Long startInstance(Long processId, Long businessId, Map<String, Object> varMap) {
        return flowRuntimeService.startFlowInstanceById(processId, businessId, varMap);
    }

    @Override
    public Long startInstance(FlowInstanceParam param) {
        Long processId = param.getProcessId();
        Long businessId = param.getBusinessId();
        Map<String, Object> varMap = param.getVarMap();
        return flowRuntimeService.startFlowInstanceById(processId, businessId, varMap);
    }

    @Override
    public void approveTask(FlowTaskParam param) {
        flowTaskService.approveTask(param);
    }

    @Override
    public void rejectTask(FlowTaskParam param) {
        flowTaskService.rejectTask(param);
    }

    @Override
    public FlowInstance getInstance(Long instanceId) {
        return flowRuntimeService.getById(instanceId);
    }

    @Override
    public List<FlowTask> listTasks(Long instanceId) {
        return flowTaskService.listTaskByInstanceId(instanceId);
    }

    @Override
    public FlowProcess getProcess(Long processId) {
        return flowProcessService.getById(processId);
    }

    @Override
    public List<FlowNode> calculateRoute(Long processId, Map<String, Object> varMap) {
        FlowProcess process = flowProcessService.getById(processId);
        if (process == null) {
            return List.of();
        }
        FlowNode flowNode = process.getFlowNode();
        // 补充父子节点关系，使用已有解析工具
        FlowParser.makeParentNode(flowNode);
        return processExecutor.calculateRoute(flowNode, varMap);
    }
}

