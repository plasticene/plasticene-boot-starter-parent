package com.plasticene.boot.flow.core.parser;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.dto.ProcessConditionGroup;
import com.plasticene.boot.flow.core.model.dto.ProcessConditionRule;
import com.plasticene.boot.flow.core.model.dto.ProcessNodeCondition;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程解析器
 * @author ZFJ
 * @date 2025/9/2
 */
public class FlowParser {
    /**
     * 统一解析流程模型节点树并设置父节点
     * @param model 模型
     * @return 模型节点树
     */
    public static FlowNode parseProcessNode(String model) {
        if (StrUtil.isBlank(model)) {
            return null;
        }
        FlowNode flowNode = JSON.parseObject(model, FlowNode.class);
        makeParentNode(flowNode);
        return flowNode;
    }


    /**
     * 设置父节点
     */
    public static void makeParentNode(FlowNode flowNode) {
        if (flowNode == null) {
            return;
        }
        FlowNode childNode = flowNode.getChildNode();
        if (childNode == null) {
            return;
        }
        childNode.setParentNode(flowNode);
        // 递归处理子节点
        makeParentNode(childNode);
        // 递归处理条件节点
        List<FlowNode> conditionNodes = flowNode.getConditionNodes();
        if (CollUtil.isNotEmpty(conditionNodes)) {
            conditionNodes.forEach(conditionNode -> {
                FlowNode conditionChildNode = conditionNode.getChildNode();
                if (conditionChildNode != null) {
                    conditionChildNode.setParentNode(flowNode);
                    makeParentNode(conditionChildNode);
                }
            });
        }
    }

    /**
     * 校验流程模型节点是否合法
     * 1.必须包含一个审批节点
     * 2. todo 校验条件节点的条件规则是否合法
     */
    public static boolean validateProcessNode(FlowNode flowNode) {
        if (flowNode == null) {
            return false;
        }
        // 检查当前节点是不是审批节点
        if (Objects.equals(flowNode.getType(), FlowNodeEnum.Type.APPROVE.getCode())) {
            return true;
        }
        // 递归检查子节点
        if (validateProcessNode(flowNode.getChildNode())) {
            return true;
        }
        // 递归检查条件节点
        List<FlowNode> conditionNodes = flowNode.getConditionNodes();
        if (CollUtil.isNotEmpty(conditionNodes)) {
            for (FlowNode conditionNode : conditionNodes) {
                if (validateProcessNode(conditionNode.getChildNode())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 根据key查找节点node
     * @param flowNode 完整的流程模型
     * @param nodeKey 节点key
     * @return 节点
     */
    public static FlowNode findNodeByKey(FlowNode flowNode, String nodeKey) {
        if (flowNode == null) {
            return null;
        }
        String key = flowNode.getKey();
        if (Objects.equals(key, nodeKey)) {
            return flowNode;
        }

        // 递归查找子节点
        FlowNode childNode = flowNode.getChildNode();
        if (childNode != null) {
            FlowNode node = findNodeByKey(childNode, nodeKey);
            if (node != null) {
                return node;
            }
        }
        // 递归查找条件结点
        List<FlowNode> conditionNodes = flowNode.getConditionNodes();
        if (CollUtil.isNotEmpty(conditionNodes)) {
            for (FlowNode conditionNode : conditionNodes) {
                FlowNode node = findNodeByKey(conditionNode.getChildNode(), nodeKey);
                if (node != null) {
                    return node;
                }
            }
        }
        return null;
    }

    /**
     * 获取当前节点执行流转的下一个节点
     * @param flowNode 当前节点
     * @return 待执行的下一个节点
     */
    public static FlowNode findExecutionNextNode(FlowNode flowNode) {
        if (flowNode == null) {
            return null;
        }
        FlowNode childNode = flowNode.getChildNode();
        if (childNode != null) {
            return childNode;
        }
        return findUpNextNode(flowNode);
    }

    /**
     * 如果当前节点的子节点为空，那么说明当前节点在条件分支里面
     * 只能向上查找应该流入的下一个节点
     */
    private static FlowNode findUpNextNode(FlowNode flowNode) {
        if (flowNode == null) {
            return null;
        }
        FlowNode parentNode = flowNode.getParentNode();
        // 条件分支下的节点
        FlowNode child = parentNode.getChildNode();
        // 找到父节点层级，流入父节点的子节点，如果子节点是当前节点，那么需要父节点再往上找
        if (child != null && child != flowNode) {
            return child;
        }
        // 再往上找
        return findUpNextNode(parentNode);
    }

    /**
     * 从流程模型中提取所有条件属性字段信息-不重复
     * @param flowNode 流程模型
     * @return 流程模型中所有条件属性字段
     */
    public static List<ProcessConditionRule> getAllProcessConditionRules(FlowNode flowNode) {
        List<ProcessConditionRule> conditionRules = new ArrayList<>();
        findConditionRules(flowNode, conditionRules);
        return conditionRules;
    }

    private static void findConditionRules(FlowNode flowNode, List<ProcessConditionRule> result) {
        if (flowNode == null) {
            return;
        }

        // 使用 Set 记录已存在的字段，避免 stream.noneMatch 重复遍历
        Set<String> exists = result.stream()
                .map(ProcessConditionRule::getField)
                .collect(Collectors.toSet());

        // 处理条件节点
        List<FlowNode> conditionNodes = flowNode.getConditionNodes();
        if (CollUtil.isNotEmpty(conditionNodes)) {
            for (FlowNode conditionNode : conditionNodes) {
                ProcessNodeCondition condition = conditionNode.getCondition();
                if (condition == null) {
                    continue;
                }
                // 收集规则
                List<ProcessConditionRule> rules = Optional.ofNullable(condition.getConditionGroups())
                        .orElse(Collections.emptyList())
                        .stream()
                        .filter(Objects::nonNull)
                        .map(ProcessConditionGroup::getConditionRules)
                        .filter(CollUtil::isNotEmpty)
                        .flatMap(List::stream)
                        .toList();

                for (ProcessConditionRule rule : rules) {
                    if (exists.add(rule.getField())) {
                        result.add(rule);
                    }
                }

                // 递归子条件节点
                findConditionRules(conditionNode.getChildNode(), result);
            }
        }

        // 递归子节点
        findConditionRules(flowNode.getChildNode(), result);
    }


//    private static void findConditionRules(FlowNode processNode, List<ProcessConditionRule> result) {
//        if (processNode == null) {
//            return;
//        }
//        // 处理条件节点
//        List<ProcessNodeCondition> conditionNodes = processNode.getConditionNodes();
//        if (CollUtil.isNotEmpty(conditionNodes)) {
//            for (ProcessNodeCondition conditionNode : conditionNodes) {
//                List<ProcessConditionGroup> conditionGroups = conditionNode.getConditionGroups();
//                if (CollUtil.isNotEmpty(conditionGroups)) {
//                    for (ProcessConditionGroup conditionGroup : conditionGroups) {
//                        List<ProcessConditionRule> rules = conditionGroup.getConditionRules();
//                        if (CollUtil.isNotEmpty(rules)) {
//                            for (ProcessConditionRule rule : rules) {
//                                String field = rule.getField();
//                                boolean b = result.stream()
//                                        .noneMatch(r -> Objects.equals(r.getField(), field));
//                                if (b) {
//                                    result.add(rule);
//                                }
//                            }
//                        }
//                    }
//                }
//                FlowNode conditionChildNode = conditionNode.getChildNode();
//                // 递归子节点
//                findConditionRules(conditionChildNode, result);
//            }
//        }
//        // 递归处理子节点
//        findConditionRules(processNode.getChildNode(), result);
//    }


}
