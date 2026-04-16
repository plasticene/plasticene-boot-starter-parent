package com.plasticene.boot.flow.core.validator;

import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.model.dto.FlowNode;

/**
 * @author ZFJ
 * @since 2026/4/15
 */
public interface NodeValidator {

    /**
     * 是否支持该节点类型
     * @param nodeType 节点类型
     * @return 是否支持
     */
    boolean support(FlowNodeEnum.Type nodeType);

    /**
     * 节点校验
     * @param node 节点
     * @param context 校验上下文
     */
    void validate(FlowNode node, ValidationContext context);
}
