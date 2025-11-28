package com.plasticene.boot.flow.core.executor;

import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.model.dto.ProcessNode;

import java.util.List;
import java.util.Map;

/**
 * @author ZFJ
 * @date 2025/11/27
 */
public interface ProcessExecutor {

    /**
     * 执行节点
     * @param instance 当前流程实例
     * @param currentNode 当前节点
     */
    void executeNode(FlowInstance instance, ProcessNode currentNode);

    /**
     * 流转到下一个节点
     * @param instance 当前流程实例
     * @param currentNode 当前节点
     */
    void moveToNextNode(FlowInstance instance, ProcessNode currentNode);

    /**
     * 根据变量推测流程节点流转路径
     * @param processNode 当前节点
     * @param varMap 流程变量
     * @return 流程节点流转路径
     */
    List<ProcessNode> calculateRoute(ProcessNode processNode, Map<String, Object> varMap);
}
