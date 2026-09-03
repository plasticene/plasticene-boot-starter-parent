package com.plasticene.boot.flow.core.model;

import java.util.Map;

/**
 * 流程表单定义。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public record FormDefinition(Long id, String tenantId, String code, String name, int version,
                             Map<String, Object> schema) {

    public FormDefinition {
        schema = schema == null ? Map.of() : Map.copyOf(schema);
    }

    public FormDefinition withId(Long value) {
        return new FormDefinition(value, tenantId, code, name, version, schema);
    }
}
