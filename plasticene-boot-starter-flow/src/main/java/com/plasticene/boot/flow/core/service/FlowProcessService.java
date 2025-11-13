package com.plasticene.boot.flow.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.flow.core.entity.FlowProcess;
import com.plasticene.boot.flow.core.param.FlowProcessParam;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowProcessService extends IService<FlowProcess> {

    /**
     * 创建流程模型
     * @param param 流程模型参数
     * @return 流程模型id
     */
    Long createFlowProcess(FlowProcessParam param);

    /**
     * 修改流程模型
     * @param param 流程模型参数
     */
    Long updateFlowProcess(FlowProcessParam param);

    /**
     * 基于流程id发布  应用于流行模型列表进行发布(先保存成草稿再发布)
     * 只有草稿状态的才能发布
     * @param processId 流程id
     */
    void releaseFlowProcess(Long processId);

    /**
     * 基于流程参数发布，应用于配置好流程模型直接发布
     * 这时候需要实现先保存，再发布
     * @param param 流程模型参数
     */
    void releaseFlowProcess(FlowProcessParam param);





}
