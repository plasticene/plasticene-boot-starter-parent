package com.plasticene.boot.flow.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.flow.core.entity.FlowProcess;
import com.plasticene.boot.flow.core.param.FlowProcessParam;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowProcessService extends IService<FlowProcess> {


    Long createFlowProcess(FlowProcessParam param);

    void updateFlowProcess(FlowProcessParam param);


}
