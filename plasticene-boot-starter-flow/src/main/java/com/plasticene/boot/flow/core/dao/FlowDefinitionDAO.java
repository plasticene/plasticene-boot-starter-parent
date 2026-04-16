package com.plasticene.boot.flow.core.dao;

import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;

/**
 * @author ZFJ
 * @since 2026/4/15
 */
public interface FlowDefinitionDAO extends BaseMapperX<FlowDefinition>{

     Integer getMaxVersion(Long modelId);
}
