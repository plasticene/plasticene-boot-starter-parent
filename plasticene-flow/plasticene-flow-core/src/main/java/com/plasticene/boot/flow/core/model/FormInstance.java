package com.plasticene.boot.flow.core.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 流程实例提交的表单数据。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public record FormInstance(Long id, String tenantId, Long definitionId, Long formDefinitionId,
                           String businessKey, Map<String, Object> data, LocalDateTime createdAt) {

    public FormInstance {
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    public FormInstance withId(Long value) {
        return new FormInstance(value, tenantId, definitionId, formDefinitionId, businessKey, data, createdAt);
    }
}
