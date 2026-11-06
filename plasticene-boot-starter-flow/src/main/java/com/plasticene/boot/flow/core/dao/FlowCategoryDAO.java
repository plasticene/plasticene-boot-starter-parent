package com.plasticene.boot.flow.core.dao;

import com.plasticene.boot.flow.core.entity.FlowCategory;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author ZFJ
 * @date 2025/11/19
 */
public interface FlowCategoryDAO extends BaseMapperX<FlowCategory> {
    @Select("SELECT * FROM flow_category WHERE org_id = #{orgId} AND id = #{id} FOR UPDATE")
    FlowCategory selectForUpdate(@Param("orgId") Long orgId, @Param("id") Long id);

    @Select("SELECT COUNT(*) FROM flow_category WHERE org_id = #{orgId} AND code = #{code}")
    long countByCode(@Param("orgId") Long orgId, @Param("code") String code);

    // 使用当前读，避免事务快照漏掉刚提交的引用；历史版本不按删除标记过滤。
    @Select("SELECT id FROM flow_model WHERE org_id = #{orgId} AND LOWER(category) = LOWER(#{code}) LIMIT 1 FOR UPDATE")
    Long findModelReference(@Param("orgId") Long orgId, @Param("code") String code);

    @Select("SELECT id FROM flow_form WHERE org_id = #{orgId} AND LOWER(category) = LOWER(#{code}) LIMIT 1 FOR UPDATE")
    Long findFormReference(@Param("orgId") Long orgId, @Param("code") String code);

    @Select("SELECT id FROM flow_definition WHERE org_id = #{orgId} AND LOWER(category) = LOWER(#{code}) LIMIT 1 FOR UPDATE")
    Long findDefinitionReference(@Param("orgId") Long orgId, @Param("code") String code);

    @Select("SELECT id FROM flow_instance WHERE org_id = #{orgId} AND LOWER(category) = LOWER(#{code}) LIMIT 1 FOR UPDATE")
    Long findInstanceReference(@Param("orgId") Long orgId, @Param("code") String code);
}
