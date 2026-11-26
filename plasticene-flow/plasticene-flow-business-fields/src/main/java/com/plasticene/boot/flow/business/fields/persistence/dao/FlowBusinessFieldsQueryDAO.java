package com.plasticene.boot.flow.business.fields.persistence.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsQuerySupport.Condition;
import com.plasticene.boot.flow.core.api.query.FlowInstanceQuery;
import com.plasticene.boot.flow.core.api.query.FlowTaskQuery;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceVO;
import com.plasticene.boot.flow.core.api.vo.FlowTaskPageVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 按预解析的业务字段条件查询个人审批分页。
 * @author ZFJ
 * @since 2026-11-20
 */
@InterceptorIgnore(tenantLine = "true")
public interface FlowBusinessFieldsQueryDAO {

    /** 按核心查询范围和业务条件分页读取个人申请。 */
    IPage<FlowInstanceVO> pageInstance(IPage<FlowInstanceVO> page, @Param("query") FlowInstanceQuery query,
            @Param("includeStatus") boolean includeStatus, @Param("filters") List<Condition> filters);

    /** 按核心查询范围和业务条件分页读取个人任务。 */
    IPage<FlowTaskPageVO> pageTask(IPage<FlowTaskPageVO> page, @Param("query") FlowTaskQuery query,
            @Param("filters") List<Condition> filters);
}
