package com.plasticene.boot.flow.business.fields;

import com.plasticene.boot.flow.business.fields.persistence.dao.FlowBusinessFieldsQueryDAO;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsFieldResolver.Kind;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsQuerySupport.Condition;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsQuerySupport.Operator;
import com.plasticene.boot.flow.core.api.query.FlowTaskQuery;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证扩展分页引用基础范围且使用类型化绑定条件。
 * @author ZFJ
 * @since 2026-11-20
 */
class FlowBusinessFieldsMapperTest {

    @Test
    void includesCoreScopeAndBindsEveryBusinessCondition() throws Exception {
        var config = new Configuration();
        for (String resource : List.of("mapper/FlowTaskMapper.xml", "mapper/FlowInstanceMapper.xml")) {
            try (var input = Resources.getResourceAsStream(resource)) {
                new XMLMapperBuilder(input, config, resource, config.getSqlFragments()).parse();
            }
        }
        config.addMapper(FlowBusinessFieldsQueryDAO.class);
        var query = new FlowTaskQuery();
        query.setAssignee(3L);
        query.setOrgId(7L);
        query.setNodeType(1);
        query.setStatuses(List.of(0));
        String taskMapper = FlowBusinessFieldsQueryDAO.class.getName() + ".pageTask";
        var sql = config.getMappedStatement(taskMapper).getBoundSql(Map.of(
                "query", query, "filters", List.of(
                        new Condition("amount", Kind.NUMBER, Operator.GT, new BigDecimal("100000"), null),
                        new Condition("supplier", Kind.TEXT, Operator.EQ, "甲公司", null)))).getSql();
        assertTrue(sql.contains("ft.assignee = ?"));
        assertTrue(sql.contains("fv0.number_value") && sql.contains("fv1.match_value"));
        assertTrue(sql.contains("fv0.org_id = fi.org_id") && sql.contains("fv1.instance_id = fi.id"));
        assertFalse(sql.contains("甲公司") || sql.contains("100000"));
        assertFalse(config.getMappedStatement(taskMapper).getBoundSql(Map.of(
                "query", query, "filters", List.of())).getSql().contains("flow_instance_field_value"));
    }
}
