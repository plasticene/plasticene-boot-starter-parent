package com.plasticene.boot.flow.core.model;

import java.time.LocalDateTime;

/**
 * 已发布且可运行的流程定义。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public record FlowDefinition(Long id, String tenantId, Long modelId, String code, String name, int version,
                             Long formDefinitionId, FlowNode rootNode, LocalDateTime publishedAt) {

    public FlowDefinition withId(Long value) {
        return new FlowDefinition(value, tenantId, modelId, code, name, version, formDefinitionId,
                rootNode, publishedAt);
    }
}
