package com.plasticene.boot.flow.core.parser;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.plasticene.boot.flow.core.dto.ProcessNode;
import com.plasticene.boot.flow.core.dto.ProcessNodeCondition;
import com.plasticene.boot.flow.core.enums.FlowProcessNodeEnum;

import java.util.List;
import java.util.Objects;

/**
 * 流程解析器
 * @author ZFJ
 * @date 2025/9/2
 */
public class FlowParser {
    public static ProcessNode parseProcessNode(String model) {
        if (StrUtil.isBlank(model)) {
            return null;
        }
        ProcessNode processNode = JSON.parseObject(model, ProcessNode.class);
        makeParentNode(processNode);
        return processNode;
    }


    public static void makeParentNode(ProcessNode processNode) {
        if (processNode == null) {
            return;
        }
        ProcessNode childNode = processNode.getChildNode();
        if (childNode == null) {
            return;
        }
        childNode.setParentNode(processNode);
        // 递归处理子节点
        makeParentNode(childNode);
        // 递归处理条件节点
        List<ProcessNodeCondition> conditionNodes = processNode.getConditionNodes();
        if (CollUtil.isNotEmpty(conditionNodes)) {
            conditionNodes.forEach(conditionNode -> {
                ProcessNode conditionChildNode = conditionNode.getChildNode();
                if (conditionChildNode != null) {
                    conditionChildNode.setParentNode(processNode);
                    makeParentNode(conditionChildNode);
                }
            });
        }
    }

    public static Boolean validateProcessNode(ProcessNode processNode) {
        if (processNode == null) {
            return false;
        }
        // 检查当前节点是不是审批节点
        if (Objects.equals(processNode.getType(), FlowProcessNodeEnum.Type.APPROVE.getCode())) {
            return true;
        }
        // 递归检查子节点
        if (validateProcessNode(processNode.getChildNode())) {
            return true;
        }
        // 递归检查条件节点
        List<ProcessNodeCondition> conditionNodes = processNode.getConditionNodes();
        if (CollUtil.isNotEmpty(conditionNodes)) {
            for (ProcessNodeCondition conditionNode : conditionNodes) {
                if (validateProcessNode(conditionNode.getChildNode())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static ProcessNode findNodeByKey(ProcessNode processNode, String nodeKey) {
        if (processNode == null) {
            return null;
        }
        String key = processNode.getKey();
        if (Objects.equals(key, nodeKey)) {
            return processNode;
        }

        // 递归查找子节点
        ProcessNode childNode = processNode.getChildNode();
        if (childNode != null) {
            ProcessNode node = findNodeByKey(childNode, nodeKey);
            if (node != null) {
                return node;
            }
        }
        // 递归查找条件结点
        List<ProcessNodeCondition> conditionNodes = processNode.getConditionNodes();
        if (CollUtil.isNotEmpty(conditionNodes)) {
            for (ProcessNodeCondition conditionNode : conditionNodes) {
                ProcessNode node = findNodeByKey(conditionNode.getChildNode(), nodeKey);
                if (node != null) {
                    return node;
                }
            }
        }
        return null;
    }

    public static ProcessNode findNextNode(ProcessNode processNode) {
        ProcessNode childNode = processNode.getChildNode();
        if (childNode != null) {
            return childNode;
        }
        ProcessNode parentNode = processNode.getParentNode();
        // 条件节点
        ProcessNode child = parentNode.getChildNode();
        if (child != null) {
            return child;
        }
        // 再往上找
        return findNextNode(parentNode);
    }
}
