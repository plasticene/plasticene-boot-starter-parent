package com.plasticene.boot.flow.core.impl.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasticene.boot.flow.core.impl.form.FlowFormConfig;
import java.sql.DriverManager;
import java.util.Map;

/**
 * 显式补齐各历史发布定义的自带表单变量元数据，不修改实例业务值。
 * @author ZFJ
 * @since 2026-11-16
 */
public final class FlowVarInfoMigration {
    private FlowVarInfoMigration() { }

    public static void main(String[] args) throws Exception {
        String url = required("FLOW_MIGRATION_JDBC_URL");
        String user = required("FLOW_MIGRATION_USERNAME");
        String password = required("FLOW_MIGRATION_PASSWORD");
        ObjectMapper mapper = new ObjectMapper();
        try (var connection = DriverManager.getConnection(url, user, password)) {
            connection.setAutoCommit(false);
            try {
                long afterId = 0;
                long count = 0;
                while (true) {
                    int batch = 0;
                    try (var query = connection.prepareStatement("SELECT id, form_config FROM flow_definition WHERE id > ? AND var_info IS NULL AND form_mode = 0 ORDER BY id LIMIT 200 FOR UPDATE")) {
                        query.setLong(1, afterId);
                        try (var rows = query.executeQuery()) {
                            while (rows.next()) {
                                afterId = rows.getLong("id");
                                String json = rows.getString("form_config");
                                String variableInfo = convert(afterId, json, mapper);
                                try (var update = connection.prepareStatement("UPDATE flow_definition SET var_info = ? WHERE id = ? AND var_info IS NULL AND form_mode = 0")) {
                                    update.setString(1, variableInfo);
                                    update.setLong(2, afterId);
                                    count += update.executeUpdate();
                                }
                                batch++;
                            }
                        }
                    }
                    if (batch < 200) { break; }
                }
                connection.commit();
                System.out.println("FLOW variable metadata updated: " + count);
            } catch (Exception failure) {
                connection.rollback();
                throw failure;
            }
        }
    }

    static String convert(long definitionId, String json, ObjectMapper mapper) {
        try {
            Map<String, Object> config = json == null ? Map.of() : mapper.readValue(json, new TypeReference<>() { });
            FlowFormConfig.validateMigrationSource(config);
            return mapper.writeValueAsString(FlowFormConfig.variableSchema(config));
        } catch (Exception failure) {
            throw new IllegalStateException("FLOW var_info 迁移失败：definitionId=" + definitionId
                    + "，原因：" + failure.getMessage(), failure);
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) { throw new IllegalArgumentException("Missing environment variable: " + name); }
        return value;
    }
}
