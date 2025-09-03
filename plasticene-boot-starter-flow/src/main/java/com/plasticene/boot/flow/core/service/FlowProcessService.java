package com.plasticene.boot.flow.core.service;

import com.plasticene.boot.flow.core.param.FlowProcessParam;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowProcessService {


    Long createFlowProcess(FlowProcessParam param);

    void updateFlowProcess(FlowProcessParam param);


}
