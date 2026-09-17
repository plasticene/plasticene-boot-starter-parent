package com.plasticene.boot.flow.core.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.model.dto.FlowInstanceNodeTimeDTO;
import com.plasticene.boot.flow.core.model.query.FlowInstanceQuery;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceStatisticsVO;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceVO;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowInstanceDAO extends BaseMapperX<FlowInstance> {

    FlowInstance selectInstanceForUpdate(@Param("instanceId") Long instanceId);

    IPage<FlowInstanceVO> pageInstance(Page<FlowInstanceVO> page,
                                       @Param("query") FlowInstanceQuery query,
                                       @Param("includeStatus") boolean includeStatus);

    FlowInstanceStatisticsVO statisticsInstance(@Param("query") FlowInstanceQuery query,
                                                @Param("includeStatus") boolean includeStatus);

    List<FlowInstanceNodeTimeDTO> listCurrentNodeStartTimes(@Param("orgId") Long orgId,
                                                           @Param("instanceIds") List<Long> instanceIds);
}
