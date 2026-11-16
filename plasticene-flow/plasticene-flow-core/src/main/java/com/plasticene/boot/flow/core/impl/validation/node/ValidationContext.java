package com.plasticene.boot.flow.core.impl.validation.node;

import lombok.Data;

import java.util.Set;

/**
 * @author ZFJ
 * @since 2026/4/15
 */
@Data
public class ValidationContext {
    private int approveNodeCount = 0;
    /** 当前被校验节点的直接父节点类型。 */
    private Integer parentNodeType;
    /** 本次发布可引用的审批变量字段。 */
    private Set<String> variableFields;
}
