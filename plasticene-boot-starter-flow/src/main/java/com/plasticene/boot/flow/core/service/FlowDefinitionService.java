package com.plasticene.boot.flow.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.flow.core.entity.FlowDefinition;

/**
 * @author ZFJ
 * @since 2026/4/15
 */
public interface FlowDefinitionService extends IService<FlowDefinition> {

    /**
     * 获取最大的版本号
     * @param modelId 模型id
     * @return 最大版本号
     */
    int getMaxVersion(Long modelId);
}
