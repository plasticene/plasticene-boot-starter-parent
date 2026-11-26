package com.plasticene.boot.flow.business.fields.persistence.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.plasticene.boot.flow.business.fields.persistence.entity.FlowInstanceFieldValue;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Select;

/**
 * 读写业务字段扩展表并检查迁移状态。
 * @author ZFJ
 * @since 2026-11-20
 */
@InterceptorIgnore(tenantLine = "true")
public interface FlowInstanceFieldValueDAO extends BaseMapperX<FlowInstanceFieldValue> {

    /** 检查业务字段扩展表已经部署。 */
    @Select("SELECT count(*) FROM flow_instance_field_value WHERE 1 = 0")
    int checkSchema();
}
