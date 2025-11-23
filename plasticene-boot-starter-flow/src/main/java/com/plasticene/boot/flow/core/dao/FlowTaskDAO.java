package com.plasticene.boot.flow.core.dao;

import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Param;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowTaskDAO extends BaseMapperX<FlowTask> {

    /**
     * 并行分支任务完成的分支数量+1
     * @param instanceId 实例id
     * @param nodeKey 并行分支key
     */
    void incrementCompletedBranch(@Param("instanceId") Long instanceId, @Param("nodeKey") String nodeKey);
}
