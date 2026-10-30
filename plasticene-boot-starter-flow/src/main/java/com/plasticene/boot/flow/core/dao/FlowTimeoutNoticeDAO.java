package com.plasticene.boot.flow.core.dao;

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
public interface FlowTimeoutNoticeDAO extends BaseMapperX<FlowTimeoutNotice> {
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    @Select("SELECT * FROM flow_timeout_notice WHERE id = #{id}FOR UPDATE")
    FlowTimeoutNotice lock(@Param("id") Long id);
}
