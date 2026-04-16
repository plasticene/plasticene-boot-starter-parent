package com.plasticene.boot.flow.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.flow.core.dao.FlowDefinitionDAO;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * @author ZFJ
 * @since 2026/4/15
 */
@Service
public class FlowDefinitionServiceImpl extends ServiceImpl<FlowDefinitionDAO, FlowDefinition> implements FlowDefinitionService {
    @Resource
    private FlowDefinitionDAO flowDefinitionDAO;
    @Override
    public int getMaxVersion(Long modelId) {
        Integer maxVersion = flowDefinitionDAO.getMaxVersion(modelId);
        return maxVersion == null ? 0 : maxVersion;
    }
}
