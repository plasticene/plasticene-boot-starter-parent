package com.plasticene.boot.flow.core.dao;

import com.plasticene.boot.flow.core.entity.FlowModel;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Param;

/**
 *
 * <p> 工作流-流程模型表 </p>
 *
 * @author ZFJ
 * @since 2026-04-13
 */

public interface FlowModelDAO extends BaseMapperX<FlowModel> {

    FlowModel selectFlowModelForUpdate(Long id);

    long countByCode(@Param("orgId") Long orgId, @Param("code") String code,
                     @Param("excludeId") Long excludeId);



}
