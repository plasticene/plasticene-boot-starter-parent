package com.plasticene.boot.flow.core.validator;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author ZFJ
 * @since 2026/4/15
 */
@Component
public class FlowNodeValidator {

    @Autowired
    private List<NodeValidator> validators;

    public void validate(FlowNode flowNode) {
        if (flowNode == null) {
            throw new BizException("流程模型节点配置不能为空");
        }
        Set<String> nodeKeySet = new HashSet<>();
        ValidationContext context = new ValidationContext();
        traverse(flowNode, nodeKeySet, context);
        if (context.getApproveNodeCount() == 0) {
            throw new BizException("流程模型节点配置至少要有一个审批节点");
        }
    }


    private void traverse(FlowNode node, Set<String> nodeKeySet, ValidationContext context) {
        if (node == null) {
            return;
        }
        String key = node.getKey();
        if (StrUtil.isBlank(key)) {
            throw new BizException("流程模型节点配置: {0}, key不能为空", node.getName());
        }
        if (nodeKeySet.contains(key)) {
            throw new BizException("流程模型节点配置: {0}, key不能重复", node.getName());
        }
        // 校验节点配置
        validators.stream()
                .filter(v -> v.support(FlowNodeEnum.Type.getType(node.getType())))
                .forEach(v -> v.validate(node, context));
        // 递归子节点
        traverse(node.getChildNode(), nodeKeySet, context);
        // 递归条件节点
        if (CollUtil.isNotEmpty(node.getConditionNodes())) {
            node.getConditionNodes().forEach(conditionNode -> traverse(conditionNode, nodeKeySet, context));
        }
    }




}
