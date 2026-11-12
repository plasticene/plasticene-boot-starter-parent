package com.plasticene.boot.flow.core.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.model.dto.FlowInstanceNodeTimeDTO;
import com.plasticene.boot.flow.core.model.query.FlowInstanceQuery;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceStatisticsVO;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceVO;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
// 支持显式身份调用及后台任务，避免依赖请求线程的租户上下文。
// 跳过自动租户过滤后，组织隔离由引擎的 orgId 查询条件或实例归属校验保障。
@InterceptorIgnore(tenantLine = "true")
public interface FlowInstanceDAO extends BaseMapperX<FlowInstance> {

    /** 在流程事务内递增指定组织实例的事件序号，同时持有实例行锁。 */
    int incrementEventSequence(@Param("orgId") Long orgId, @Param("instanceId") Long instanceId);

    /** 读取当前事务内已递增的实例事件序号。 */
    Long selectEventSequence(@Param("orgId") Long orgId, @Param("instanceId") Long instanceId);

    FlowInstance selectInstanceForUpdate(@Param("instanceId") Long instanceId);

    IPage<FlowInstanceVO> pageInstance(Page<FlowInstanceVO> page,
                                       @Param("query") FlowInstanceQuery query,
                                       @Param("includeStatus") boolean includeStatus);

    FlowInstanceStatisticsVO statisticsInstance(@Param("query") FlowInstanceQuery query,
                                                @Param("includeStatus") boolean includeStatus);

    List<FlowInstanceNodeTimeDTO> listCurrentNodeStartTimes(@Param("orgId") Long orgId,
                                                           @Param("instanceIds") List<Long> instanceIds);
}
