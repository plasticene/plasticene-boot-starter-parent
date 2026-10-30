package com.plasticene.boot.flow.core.validator;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.dto.FlowTimeoutRule;

/**
 * @author ZFJ
 * @since 2026/4/15
 */
public class ApproveNodeValidator implements NodeValidator{
    @Override
    public boolean support(FlowNodeEnum.Type nodeType) {
        return FlowNodeEnum.Type.APPROVE.equals(nodeType);
    }

    @Override
    public void validate(FlowNode node, ValidationContext context) {
        if (node.getApproveType() == null) {
            throw new BizException("审批节点: {0}, 审批类型不能为空", node.getKey());
        }
        if (node.getAssigneeType() == null) {
            throw new BizException("审批节点: {0}, 处理人类型不能为空", node.getKey());
        }

        if (node.getTimeoutMode() == FlowTimeoutRule.Mode.CUSTOM) {
            if (node.getTimeoutRule() == null) {
                throw new BizException("审批节点: {0}, 自定义超时规则不能为空", node.getKey());
            }
            node.getTimeoutRule().validate();
        }

        context.setApproveNodeCount(context.getApproveNodeCount() + 1);

    }
}
