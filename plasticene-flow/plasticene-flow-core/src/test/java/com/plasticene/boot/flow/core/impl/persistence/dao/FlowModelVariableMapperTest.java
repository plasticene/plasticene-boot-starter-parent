package com.plasticene.boot.flow.core.impl.persistence.dao;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowModel;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证草稿变量定义的实际 SQL 参数和查询结果 JSON 映射。
 * @author ZFJ
 * @since 2026-11-17
 */
class FlowModelVariableMapperTest {
    @Test
    void shouldBindDraftVariablesAsJsonAndReadBothDetailAndLockedModel() throws Exception {
        var configuration = new MybatisConfiguration();
        configuration.addMapper(FlowModelDAO.class);
        String resource = "mapper/FlowModelMapper.xml";
        try (var input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        var field = new FlowVariableSchema.Field();
        field.setField("amount");
        field.setTitle("金额");
        field.setType(1);
        var schema = new FlowVariableSchema();
        schema.setFields(List.of(field));
        var model = new FlowModel();
        model.setVarInfo(schema);
        var statement = configuration.getMappedStatement(FlowModelDAO.class.getName() + ".insert");
        var sql = statement.getBoundSql(model);
        int index = -1;
        for (int i = 0; i < sql.getParameterMappings().size(); i++) {
            if (sql.getParameterMappings().get(i).getProperty().equals("varInfo")) {
                index = i + 1;
            }
        }
        assertTrue(index > 0);
        var jdbc = mock(PreparedStatement.class);
        new DefaultParameterHandler(statement, model, sql).setParameters(jdbc);
        var value = ArgumentCaptor.forClass(String.class);
        verify(jdbc).setString(eq(index), value.capture());
        assertEquals(schema, new ObjectMapper().readValue(value.getValue(), FlowVariableSchema.class));
        for (String query : List.of("selectById", "selectFlowModelForUpdate")) {
            var mapping = configuration.getMappedStatement(FlowModelDAO.class.getName() + "." + query)
                    .getResultMaps().getFirst().getResultMappings().stream()
                    .filter(item -> item.getProperty().equals("varInfo")).findFirst().orElseThrow();
            var row = mock(ResultSet.class);
            when(row.getString("var_info")).thenReturn(value.getValue());
            assertEquals(schema, mapping.getTypeHandler().getResult(row, "var_info"));
        }
    }

    @Test
    void shouldPreserveOmittedVariablesAndAllowExplicitEmptySchemaOnUpdate() {
        var configuration = new MybatisConfiguration();
        configuration.addMapper(FlowModelDAO.class);
        var statement = configuration.getMappedStatement(FlowModelDAO.class.getName() + ".updateById");
        var model = new FlowModel();
        model.setId(11L);
        model.setName("业务流程");
        assertFalse(statement.getBoundSql(Map.of("et", model)).getSql().contains("var_info"));
        model.setVarInfo(new FlowVariableSchema());
        assertTrue(statement.getBoundSql(Map.of("et", model)).getSql().contains("var_info"));
    }
}
