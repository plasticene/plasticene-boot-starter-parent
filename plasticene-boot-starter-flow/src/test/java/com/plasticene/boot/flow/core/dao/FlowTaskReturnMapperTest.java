package com.plasticene.boot.flow.core.dao;

import com.plasticene.boot.flow.core.model.query.FlowTaskQuery;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证退回后的有效网关与待办查询使用实际Mapper SQL排除旧记录。
 * @author ZFJ
 * @since 2026-11-02
 */
class FlowTaskReturnMapperTest {
    @Test
    void shouldIncrementOnlyEffectiveGatewayRecord() throws Exception {
        Configuration configuration = load("mapper/FlowTaskMapper.xml");
        String sql = configuration.getMappedStatement(FlowTaskDAO.class.getName() + ".incrementCompletedBranch")
                .getBoundSql(Map.of("instanceId", 1L, "nodeKey", "g")).getSql();
        assertTrue(sql.contains("return_task_id IS NULL"));
        assertTrue(sql.contains("is_delete = 0"));
    }

    @Test
    void shouldKeepHistoricalDoneTasksButExcludeInvalidRunningTasks() throws Exception {
        Configuration configuration = load("mapper/FlowTaskMapper.xml");
        FlowTaskQuery query = new FlowTaskQuery();
        query.setOrgId(9L);
        query.setAssignee(8L);
        query.setNodeType(1);
        String sql = configuration.getMappedStatement(FlowTaskDAO.class.getName() + ".pageTask")
                .getBoundSql(Map.of("query", query)).getSql();
        assertTrue(sql.contains("ft.status != 0 OR ft.return_task_id IS NULL"));
    }

    @Test
    void shouldUseEffectiveRoundForCurrentNodeElapsedTime() throws Exception {
        Configuration configuration = load("mapper/FlowInstanceMapper.xml");
        String sql = configuration.getMappedStatement(FlowInstanceDAO.class.getName() + ".listCurrentNodeStartTimes")
                .getBoundSql(Map.of("orgId", 9L, "instanceIds", java.util.List.of(1L))).getSql();
        assertTrue(sql.contains("ft.return_task_id IS NULL"));
    }

    private Configuration load(String resource) throws Exception {
        Configuration configuration = new Configuration();
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream);
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration;
    }
}
