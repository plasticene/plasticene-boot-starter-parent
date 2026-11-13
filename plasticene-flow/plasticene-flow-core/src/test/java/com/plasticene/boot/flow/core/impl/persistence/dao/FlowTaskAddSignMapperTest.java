package com.plasticene.boot.flow.core.impl.persistence.dao;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTaskAddSign;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 验证加签人员在实际Mapper参数与结果映射中以逗号分隔文本保存并保持Long类型和顺序。
 * @author ZFJ
 * @since 2026-11-03
 */
class FlowTaskAddSignMapperTest {
    @Test
    void shouldBindAssigneesAsCommaSeparatedTextThroughGeneratedInsertMapping() throws Exception {
        MybatisConfiguration configuration = configuration();
        FlowTaskAddSign batch = new FlowTaskAddSign();
        batch.setAssigneeIds(List.of(11L, 10L));
        var statement = configuration.getMappedStatement(FlowTaskAddSignDAO.class.getName() + ".insert");
        var sql = statement.getBoundSql(batch);
        int index = -1;
        for (int i = 0; i < sql.getParameterMappings().size(); i++) {
            if (sql.getParameterMappings().get(i).getProperty().equals("assigneeIds")) index = i + 1;
        }
        assertTrue(index > 0, "实际INSERT必须包含加签人员字段");
        PreparedStatement jdbc = mock(PreparedStatement.class);
        new DefaultParameterHandler(statement, batch, sql).setParameters(jdbc);
        var value = ArgumentCaptor.forClass(String.class);
        verify(jdbc).setString(eq(index), value.capture());
        assertEquals("11,10", value.getValue());
    }

    @Test
    void shouldReadCommaSeparatedAssigneesWithLongTypeAndOriginalOrder() throws Exception {
        var mapping = configuration().getMappedStatement(FlowTaskAddSignDAO.class.getName() + ".selectById")
                .getResultMaps().getFirst().getResultMappings().stream()
                .filter(field -> field.getProperty().equals("assigneeIds")).findFirst().orElseThrow();
        ResultSet row = mock(ResultSet.class);
        when(row.getString("assignee_ids")).thenReturn("11,10");
        Object result = mapping.getTypeHandler().getResult(row, "assignee_ids");
        assertEquals(List.of(11L, 10L), result);
    }

    private MybatisConfiguration configuration() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.addMapper(FlowTaskAddSignDAO.class);
        return configuration;
    }
}
