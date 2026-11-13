package com.plasticene.boot.flow.core.impl.persistence.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTaskAddSign;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;

/**
 * 持久化审批任务加签批次。
 * @author ZFJ
 * @since 2026-11-03
 */
// 支持显式身份调用及后台任务，避免依赖请求线程的租户上下文。
// 跳过自动租户过滤后，组织隔离由引擎的 orgId 查询条件或实例归属校验保障。
@InterceptorIgnore(tenantLine = "true")
public interface FlowTaskAddSignDAO extends BaseMapperX<FlowTaskAddSign> {
}
