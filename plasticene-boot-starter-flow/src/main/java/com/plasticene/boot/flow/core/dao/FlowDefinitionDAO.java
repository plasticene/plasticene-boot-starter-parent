package com.plasticene.boot.flow.core.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;

/**
 * @author ZFJ
 * @since 2026/4/15
 */
// 支持显式身份调用及后台任务，避免依赖请求线程的租户上下文。
// 跳过自动租户过滤后，组织隔离由引擎的 orgId 查询条件或实例归属校验保障。
@InterceptorIgnore(tenantLine = "true")
public interface FlowDefinitionDAO extends BaseMapperX<FlowDefinition>{

     Integer getMaxVersion(Long modelId);
}
