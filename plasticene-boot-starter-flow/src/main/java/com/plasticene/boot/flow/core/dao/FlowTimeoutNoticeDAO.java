package com.plasticene.boot.flow.core.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.plasticene.boot.flow.core.entity.FlowTimeoutNotice;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 查询并锁定待发送超时通知。
 * @author ZFJ
 * @since 2026-10-22
 */
// 支持显式身份调用及后台任务，避免依赖请求线程的租户上下文。
// 跳过自动租户过滤后，组织隔离由引擎的 orgId 查询条件或实例归属校验保障。
@InterceptorIgnore(tenantLine = "true")
public interface FlowTimeoutNoticeDAO extends BaseMapperX<FlowTimeoutNotice> {
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    @Select("SELECT * FROM flow_timeout_notice WHERE id = #{id} FOR UPDATE")
    FlowTimeoutNotice lock(@Param("id") Long id);
}
