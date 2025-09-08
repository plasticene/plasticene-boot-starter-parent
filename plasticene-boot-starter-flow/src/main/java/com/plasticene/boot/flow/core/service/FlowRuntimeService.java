package com.plasticene.boot.flow.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.flow.core.dto.ProcessNode;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowRuntimeService extends IService<FlowInstance> {

    void startFlowInstanceById(Long processId);

    FlowInstance selectInstanceForUpdate(Long instanceId);

    void updateInstanceCurrentNode(Long instanceId, ProcessNode currentNode);

    void endInstance(FlowInstance instance, ProcessNode currentNode, FlowInstanceStatusEnum status);

}
