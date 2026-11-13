package com.plasticene.boot.flow.core.impl.persistence.dao;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowCategory;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证分类隔离、行锁与历史引用检查使用的实际映射SQL。
 * @author ZFJ
 * @since 2026-11-06
 */
class FlowCategoryMapperTest {
    @Test
    void shouldUseRenamedTableAndCompanyScopedLocks() {
        var info = TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "category"), FlowCategory.class);
        assertEquals("flow_category", info.getTableName());
        var config = new MybatisConfiguration();
        config.addMapper(FlowCategoryDAO.class);
        for (String method : new String[]{"selectForUpdate", "selectByCodeForUpdate", "countByCode"}) {
            String sql = sql(config, method);
            assertTrue(sql.contains("FROM flow_category"));
            assertTrue(sql.contains("org_id = ?"));
            if (!method.equals("countByCode")) {
                assertTrue(sql.endsWith("FOR UPDATE"));
            }
            assertFalse(sql.contains("BINARY"));
            assertFalse(sql.contains("LOWER"));
            if (!method.equals("selectForUpdate")) {
                assertTrue(sql.contains("code = ?"));
            }
        }
    }

    @Test
    void shouldUseCurrentReadsWithoutFilteringHistoricalReferences() {
        var config = new MybatisConfiguration();
        config.addMapper(FlowCategoryDAO.class);
        for (String method : new String[]{"findModelReference", "findFormReference", "findDefinitionReference", "findInstanceReference"}) {
            String sql = sql(config, method);
            assertTrue(sql.contains("org_id = ?"));
            assertTrue(sql.contains("category = ?"));
            assertFalse(sql.contains("LOWER"));
            assertTrue(sql.endsWith("FOR UPDATE"));
            assertFalse(sql.contains("is_delete"));
            assertFalse(sql.contains("status"));
        }
    }

    private String sql(MybatisConfiguration config, String method) {
        return config.getMappedStatement(FlowCategoryDAO.class.getName() + "." + method)
                .getBoundSql(Map.of("orgId", 9L, "id", 21L, "code", "office")).getSql();
    }
}
