package com.plasticene.boot.flow.core.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.plasticene.boot.flow.core.entity.FlowFormChangeRecord;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;

/**
 * 持久化流程表单变更记录。
 * @author ZFJ
 * @since 2026-11-04
 */
// 支持显式身份调用及后台任务，避免依赖请求线程的租户上下文。
// 跳过自动租户过滤后，组织隔离由引擎的 orgId 查询条件或实例归属校验保障。
@InterceptorIgnore(tenantLine = "true")
public interface FlowFormChangeRecordDAO extends BaseMapperX<FlowFormChangeRecord> {
}
