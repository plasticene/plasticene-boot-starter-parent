package com.plasticene.boot.flow.core.dao;

import com.plasticene.boot.flow.core.entity.FlowModel;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;

/**
 *
 * <p> 工作流-流程模型表 </p>
 *
 * @author ZFJ
 * @since 2026-04-13
 */

public interface FlowModelDAO extends BaseMapperX<FlowModel> {

    FlowModel selectFlowModelForUpdate(Long id);



}
