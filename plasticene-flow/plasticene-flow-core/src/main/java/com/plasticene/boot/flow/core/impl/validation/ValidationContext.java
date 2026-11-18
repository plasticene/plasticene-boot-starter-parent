package com.plasticene.boot.flow.core.impl.validation;

import lombok.Data;

import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import java.util.Map;

/**
 * @author ZFJ
 * @since 2026/4/15
 */
@Data
public class ValidationContext {
    private int approveNodeCount = 0;
    /** 当前被校验节点的直接父节点类型。 */
    private Integer parentNodeType;
    /** 本次发布可引用的审批变量定义，按字段标识索引。 */
    private Map<String, FlowVariableSchema.Field> variableDefinitions;
}
