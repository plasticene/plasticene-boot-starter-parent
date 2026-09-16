package com.plasticene.boot.flow.core.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.model.query.FlowTaskQuery;
import com.plasticene.boot.flow.core.model.vo.FlowTaskPageVO;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Param;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowTaskDAO extends BaseMapperX<FlowTask> {

    IPage<FlowTaskPageVO> pageTask(Page<FlowTaskPageVO> page, @Param("query") FlowTaskQuery query);

    /**
     * 并行分支任务完成的分支数量+1
     * @param instanceId 实例id
     * @param nodeKey 并行分支key
     */
    void incrementCompletedBranch(@Param("instanceId") Long instanceId, @Param("nodeKey") String nodeKey);
}
