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
        traverse(flowNode, nodeKeySet, context, null);
        if (context.getApproveNodeCount() == 0) {
            throw new BizException("流程模型节点配置至少要有一个审批节点");
        }
    }


    private void traverse(FlowNode node, Set<String> nodeKeySet, ValidationContext context, Integer parentType) {
        if (node == null) {
            return;
        }
        String key = node.getKey();
        if (StrUtil.isBlank(key)) {
            throw new BizException("流程模型节点配置: {0}, key不能为空", node.getName());
        }
        if (!nodeKeySet.add(key)) {
            throw new BizException("流程模型节点配置: {0}, key不能重复", node.getName());
        }
        context.setParentNodeType(parentType);
        if (FlowNodeEnum.Type.PARALLEL_BRANCH.getCode().equals(node.getType())) {
            List<FlowNode> branches = node.getConditionNodes();
            if (CollUtil.isEmpty(branches) || branches.size() < 2
                    || branches.stream().anyMatch(branch -> branch == null
                        || !FlowNodeEnum.Type.CONDITION_NODE.getCode().equals(branch.getType()))
                    || branches.stream().filter(branch -> branch.getCondition() != null
                        && Boolean.TRUE.equals(branch.getCondition().getIsDefault())).count() != 1
                    || branches.getLast().getCondition() == null
                    || !Boolean.TRUE.equals(branches.getLast().getCondition().getIsDefault())) {
                throw new BizException("并行分支: {0}, 必须包含普通分支和位于末尾的唯一兜底分支", key);
            }
        }
        // 校验节点配置
        validators.stream()
                .filter(v -> v.support(FlowNodeEnum.Type.getType(node.getType())))
                .forEach(v -> v.validate(node, context));
        // 递归子节点
        traverse(node.getChildNode(), nodeKeySet, context, node.getType());
        // 递归条件节点
        if (CollUtil.isNotEmpty(node.getConditionNodes())) {
            node.getConditionNodes().forEach(conditionNode -> traverse(conditionNode, nodeKeySet, context, node.getType()));
        }
    }




}
