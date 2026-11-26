package com.plasticene.boot.flow.core.api.spi;

import com.plasticene.boot.flow.core.model.FlowNode;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;

import java.util.Map;

/**
 * 提供业务字段扩展在核心事务中的同步挂接点。
 * @author ZFJ
 * @since 2026-11-20
 */
public interface FlowBusinessFieldsLifecycle {

    /** 校验发布上下文并返回应由核心保存的变量快照。 */
    FlowVariableSchema prepare(Publication publication);

    /** 按实例最终变量同步检索数据。 */
    void sync(InstanceData instance);

    /**
     * 提供核心验证后准备写入的模型定义。
     * @author ZFJ
     * @since 2026-11-20
     */
    record Publication(Long orgId, Long modelId, Long categoryId, String category, String processCode,
                       Integer formMode, FlowNode node, FlowVariableSchema schema) {
    }

    /**
     * 提供同一事务中的最终实例字段值。
     * @author ZFJ
     * @since 2026-11-20
     */
    record InstanceData(Long orgId, Long instanceId, Long actorId,
                        FlowVariableSchema schema, Map<String, Object> variables) {
    }
}
