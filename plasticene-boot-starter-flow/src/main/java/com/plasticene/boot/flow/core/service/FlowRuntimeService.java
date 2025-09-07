package com.plasticene.boot.flow.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.flow.core.entity.FlowInstance;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowRuntimeService extends IService<FlowInstance> {

    FlowInstance startFlowInstanceById(Long processId);

    FlowInstance selectInstanceForUpdate(Long instanceId);

}
