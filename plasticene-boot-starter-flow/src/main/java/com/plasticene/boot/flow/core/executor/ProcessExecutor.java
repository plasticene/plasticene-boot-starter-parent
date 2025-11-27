package com.plasticene.boot.flow.core.executor;

import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.model.dto.ProcessNode;

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
}
