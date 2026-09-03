package com.plasticene.boot.flow.core.model;

import java.time.LocalDateTime;

/**
 * 可编辑的流程模型。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public record FlowModel(Long id, String tenantId, String code, String name, FlowEnums.ModelStatus status,
                        Long formDefinitionId, FlowNode modelNode, Long activeDefinitionId,
                        int activeVersion, LocalDateTime publishedAt) {

    public FlowModel withId(Long value) {
        return new FlowModel(value, tenantId, code, name, status, formDefinitionId, modelNode,
                activeDefinitionId, activeVersion, publishedAt);
    }

    public FlowModel published(FlowDefinition definition, LocalDateTime time) {
        return new FlowModel(id, tenantId, code, name, FlowEnums.ModelStatus.PUBLISHED, formDefinitionId,
                modelNode, definition.id(), definition.version(), time);
    }
}
