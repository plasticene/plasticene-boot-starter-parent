package com.plasticene.boot.flow.core.api.spi;

import com.plasticene.boot.flow.core.model.FlowVariableSchema;

/**
 * 由宿主提供组织和流程编码对应的业务变量定义。
 * @author ZFJ
 * @since 2026-11-16
 */
@FunctionalInterface
public interface FlowBusinessVariableProvider {
    FlowVariableSchema getSchema(Long orgId, String processCode);
}
