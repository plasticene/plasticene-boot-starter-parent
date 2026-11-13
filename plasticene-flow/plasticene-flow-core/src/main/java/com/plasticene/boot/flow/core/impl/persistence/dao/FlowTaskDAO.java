package com.plasticene.boot.flow.core.impl.persistence.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTask;
import com.plasticene.boot.flow.core.api.query.FlowTaskQuery;
import com.plasticene.boot.flow.core.api.vo.FlowTaskPageVO;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Param;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
// 支持显式身份调用及后台任务，避免依赖请求线程的租户上下文。
// 跳过自动租户过滤后，组织隔离由引擎的 orgId 查询条件或实例归属校验保障。
@InterceptorIgnore(tenantLine = "true")
public interface FlowTaskDAO extends BaseMapperX<FlowTask> {

    IPage<FlowTaskPageVO> pageTask(Page<FlowTaskPageVO> page, @Param("query") FlowTaskQuery query);

    /** 按数量累计当前有效网关的已完成分支。 */
    void addCompletedBranches(@Param("instanceId") Long instanceId, @Param("nodeKey") String nodeKey,
                              @Param("count") int count);

    /**
     * 并行分支任务完成的分支数量+1
     * @param instanceId 实例id
     * @param nodeKey 并行分支key
     */
    void incrementCompletedBranch(@Param("instanceId") Long instanceId, @Param("nodeKey") String nodeKey);
}
