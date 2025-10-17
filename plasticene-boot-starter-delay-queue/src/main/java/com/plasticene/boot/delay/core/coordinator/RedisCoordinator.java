package com.plasticene.boot.delay.core.coordinator;

import cn.hutool.core.collection.CollUtil;
import com.plasticene.boot.delay.core.constant.DelayConstant;
import com.plasticene.boot.delay.core.prop.DelayProperties;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * 基于redis实现分布式集群协调服务
 * @author ZFJ
 * @date 2025/10/16
 */
public class RedisCoordinator implements Coordinator {
    private static final Logger logger = LoggerFactory.getLogger(RedisCoordinator.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private DelayProperties delayProperties;

    // 心跳续期执行器
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();



    /**
     * 注册节点
     * @return nodeId节点标识
     */
    @Override
    public String registerNode() {
        // 生成节点id
        String nodeId = UUID.randomUUID().toString().replace("-", "");
        // 注册节点
        stringRedisTemplate.opsForList().rightPush(DelayConstant.NODES_KEY, nodeId);
        // 设置保活标识
        setNodeHeartbeat(nodeId);
        return nodeId;
    }

    /**
     * 注销节点
     * @param nodeId 节点id
     */
    @Override
    public void unRegisterNode(String nodeId) {
        // 在集群中下线节点
        stringRedisTemplate.opsForList().remove(DelayConstant.NODES_KEY, 1, nodeId);
        // 删除节点
        stringRedisTemplate.delete(DelayConstant.NODE_HEARTBEAT_KEY_PREFIX + nodeId);
        heartbeatExecutor.shutdown();
    }

    /**
     * 获取存活的节点
     */
    @Override
    public List<String> getActiveNodes() {
        List<String> nodes = stringRedisTemplate.opsForList().range(DelayConstant.NODES_KEY, 0, -1);
        if (CollUtil.isEmpty(nodes)) {
            return new ArrayList<>();
        }
        List<String> activeNodes = new ArrayList<>();
        for (String nodeId : nodes) {
            // 判断节点是否存活
            Boolean exist = stringRedisTemplate.hasKey(DelayConstant.NODE_HEARTBEAT_KEY_PREFIX + nodeId);
            if (exist) {
                activeNodes.add(nodeId);
            }
        }
        return activeNodes;
    }

    /**
     * 心跳续期保活，延迟一个周期开始心跳续期
     * @param nodeId 节点id
     */
    @Override
    public void heartBeat(String nodeId) {
        heartbeatExecutor.scheduleAtFixedRate(() -> setNodeHeartbeat(nodeId),
                delayProperties.getHeartbeatPeriod(), delayProperties.getHeartbeatPeriod(), TimeUnit.SECONDS);
    }

    /**
     * 集群健康检查
     */
    @Override
    public void checkClusterHealth(BiConsumer<List<String>, List<String>> consumer) {
        // 分布式锁，保证集群中只有一个节点在执行健康检查
        RLock lock = redissonClient.getLock(DelayConstant.NODES_HEALTH_KEY);
        try {
            // 没有获得锁，说明其他节点在执行监控检查，当前节点就不用了，直接返回
            boolean isLock = lock.tryLock();
            if (!isLock) {
                return;
            }
            // 获取集群节点信息
            List<String> nodes = stringRedisTemplate.opsForList().range(DelayConstant.NODES_KEY, 0, -1);
            if (CollUtil.isEmpty(nodes)) {
                return;
            }
            List<String> deadNodes = new ArrayList<>();
            // 健康检查
            for (String nodeId : nodes) {
                // 判断节点是否存活
                Boolean exist = stringRedisTemplate.hasKey(DelayConstant.NODE_HEARTBEAT_KEY_PREFIX + nodeId);
                if (!exist) {
                    deadNodes.add(nodeId);
                }
            }
            if (CollUtil.isEmpty(deadNodes)) {
                return;
            }
            // 删除原列表然后重新写入
            // stringRedisTemplate.delete(DelayConstant.NODES_KEY);
            // stringRedisTemplate.opsForList().rightPushAll(DelayConstant.NODES_KEY, aliveNodes);
            // 上面先删除再插入无法保证操作原子性，并发情况下可能导致读取存活节点是空的
            // 所以使用下面循环单个删除，节点数量少没啥性能问题
            deadNodes.forEach(nodeId -> {
                // 每个deadNode都是唯一的，所以只需删除1次
                stringRedisTemplate.opsForList().remove(DelayConstant.NODES_KEY, 1, nodeId);
            });

            // 有节点下线，将下线节点的数据移到当前节点, 重平衡的核心逻辑在这里
            consumer.accept(nodes, deadNodes);
        } catch (Exception e) {
            logger.error("check nodes health: ", e);
        } finally {
            lock.unlock();
        }


    }

    /**
     * 节点保活 过期时间是心跳需求周期的2倍，保证一个心跳续期周期内节点不会过期
     */
    private void setNodeHeartbeat(String nodeId) {
        stringRedisTemplate.opsForValue().set(DelayConstant.NODE_HEARTBEAT_KEY_PREFIX + nodeId,
                "1", delayProperties.getHeartbeatPeriod() * 2, TimeUnit.SECONDS);
    }

}

