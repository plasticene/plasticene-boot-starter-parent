package com.plasticene.boot.flow.core.api.spi;

import com.plasticene.boot.flow.core.model.FlowVariableSchema;

/**
 * 由宿主提供组织和流程编码对应的业务变量定义。
 * @author ZFJ
 * @since 2026-11-16
 */
@FunctionalInterface
public interface FlowBusinessVariableProvider {
    /**
     * 查询宿主维护的业务变量定义；没有业务变量时可以返回空定义或 null。
     * 未配置此 SPI 或返回 null 时，FLOW 使用空契约，发布时仍检查全部变量引用。
     * @param orgId 模型所属组织 ID
     * @param processCode 流程编码
     * @return 业务变量定义，无变量时可为空；提供器执行异常继续向上传递
     */
    FlowVariableSchema getSchema(Long orgId, String processCode);
}
