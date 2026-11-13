package com.plasticene.boot.flow.core.impl.persistence.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowModel;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Param;

/**
 *
 * <p> 工作流-流程模型表 </p>
 *
 * @author ZFJ
 * @since 2026-04-13
 */

// 支持显式身份调用及后台任务，避免依赖请求线程的租户上下文。
// 跳过自动租户过滤后，组织隔离由引擎的 orgId 查询条件或实例归属校验保障。
@InterceptorIgnore(tenantLine = "true")
public interface FlowModelDAO extends BaseMapperX<FlowModel> {

    FlowModel selectFlowModelForUpdate(Long id);

    long countByCode(@Param("orgId") Long orgId, @Param("code") String code,
                     @Param("excludeId") Long excludeId);



}
