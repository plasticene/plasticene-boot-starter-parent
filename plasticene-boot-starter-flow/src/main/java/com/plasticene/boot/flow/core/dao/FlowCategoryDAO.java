package com.plasticene.boot.flow.core.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.plasticene.boot.flow.core.entity.FlowCategory;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author ZFJ
 * @date 2025/11/19
 */
// 支持显式身份调用及后台任务，避免依赖请求线程的租户上下文。
// 跳过自动租户过滤后，组织隔离由引擎的 orgId 查询条件或实例归属校验保障。
@InterceptorIgnore(tenantLine = "true")
public interface FlowCategoryDAO extends BaseMapperX<FlowCategory> {
    @Select("SELECT * FROM flow_category WHERE org_id = #{orgId} AND id = #{id} FOR UPDATE")
    FlowCategory selectForUpdate(@Param("orgId") Long orgId, @Param("id") Long id);

    @Select("SELECT * FROM flow_category WHERE org_id = #{orgId} AND code = #{code} FOR UPDATE")
    FlowCategory selectByCodeForUpdate(@Param("orgId") Long orgId, @Param("code") String code);

    @Select("SELECT COUNT(*) FROM flow_category WHERE org_id = #{orgId} AND code = #{code}")
    long countByCode(@Param("orgId") Long orgId, @Param("code") String code);

    // 使用当前读，避免事务快照漏掉刚提交的引用；历史版本不按删除标记过滤。
    @Select("SELECT id FROM flow_model WHERE org_id = #{orgId} AND category = #{code} LIMIT 1 FOR UPDATE")
    Long findModelReference(@Param("orgId") Long orgId, @Param("code") String code);

    @Select("SELECT id FROM flow_form WHERE org_id = #{orgId} AND category = #{code} LIMIT 1 FOR UPDATE")
    Long findFormReference(@Param("orgId") Long orgId, @Param("code") String code);

    @Select("SELECT id FROM flow_definition WHERE org_id = #{orgId} AND category = #{code} LIMIT 1 FOR UPDATE")
    Long findDefinitionReference(@Param("orgId") Long orgId, @Param("code") String code);

    @Select("SELECT id FROM flow_instance WHERE org_id = #{orgId} AND category = #{code} LIMIT 1 FOR UPDATE")
    Long findInstanceReference(@Param("orgId") Long orgId, @Param("code") String code);
}
