package com.plasticene.boot.flow.core.validator;

import lombok.Data;

/**
 * @author ZFJ
 * @since 2026/4/15
 */
@Data
public class ValidationContext {
    private int approveNodeCount = 0;
    /** 当前被校验节点的直接父节点类型。 */
    private Integer parentNodeType;
}
