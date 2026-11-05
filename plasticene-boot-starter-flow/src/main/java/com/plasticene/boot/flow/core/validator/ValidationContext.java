package com.plasticene.boot.flow.core.validator;

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
    /** 本次发布可引用的基础表单字段；为空表示未启用引用校验。 */
    private Set<String> formFields;
}
