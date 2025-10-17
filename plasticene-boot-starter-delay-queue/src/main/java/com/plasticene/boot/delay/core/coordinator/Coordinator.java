package com.plasticene.boot.delay.core.coordinator;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * 协调服务接口：注册、注销服务节点，心跳机制，健康检查
 * @author ZFJ
 * @date 2025/10/16
 */
public interface Coordinator {

    /**
     * 服务节点注册
     */
    String registerNode();

    /**
     * 注销节点
     */
    void unRegisterNode(String nodeId);

    /**
     * 获取存活节点
     * @return 保活节点
     */
    List<String> getActiveNodes();

    /**
     * 心跳续期
     */
    void heartBeat(String nodeId);

    /**
     * 健康检查
     * @param consumer 下线节点任务转移
     */
    void checkClusterHealth(BiConsumer<List<String>, List<String>> consumer);

}
