package com.plasticene.boot.delay.core.constant;

/**
 * @author ZFJ
 * @date 2025/10/16
 */
public class DelayConstant {
    /**
     * 存储分布式集群节点信息
     */
    public static final String NODES_KEY = "ptc:nodes";
    /**
     * 节点心跳续期key
     */
    public static final String NODE_HEARTBEAT_KEY_PREFIX = "ptc:heartbeat:";
    /**
     * 集群健康检查分布式锁key
     */
    public static final String NODES_HEALTH_KEY = "ptc:nodes:health";
    /**
     * 存储业务的任务id
     */
    public static final String DELAY_TASK_KEY_PREFIX = "ptc:delay:task:";
    /**
     * 存储已执行的任务id
     */
    public static final String DELAY_EXECUTED_KEY = "ptc:delay:Executed";
    /**
     * 任务执行中的分布式锁key
     */
    public static final String DELAY_EXECUTING_KEY_PREFIX = "ptc:delay:executing:";
}
