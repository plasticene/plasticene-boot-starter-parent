package com.plasticene.boot.delay.core;

import cn.hutool.core.collection.CollUtil;
import com.plasticene.boot.common.executor.PlasticeneThreadExecutor;
import com.plasticene.boot.delay.core.constant.DelayConstant;
import com.plasticene.boot.delay.core.coordinator.Coordinator;
import com.plasticene.boot.delay.core.executor.DelayTaskExecutor;
import com.plasticene.boot.delay.core.prop.DelayProperties;
import com.plasticene.boot.delay.core.storage.TaskStorage;
import com.plasticene.boot.delay.core.task.DelayTask;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 分布式延时队列
 * @author ZFJ
 * @date 2025/10/17
 */
public class DistributedDelayQueue implements InitializingBean, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(DistributedDelayQueue.class);
    @Resource
    private Coordinator coordinator;
    @Resource
    private TaskStorage taskStorage;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private List<DelayTaskExecutor> taskExecutors;
    @Resource
    private DelayProperties delayProperties;

    /**  队列名称 -> 延迟队列
     *   业务队列隔离开来
     */
    private final ConcurrentMap<String, DelayQueue<DelayTask>> delayMap = new ConcurrentHashMap<>();
    /** 业务队列名称   */
    private final Set<String> queueNameSet = new HashSet<>();
    /** 当前节点存储的业务任务id  */
    private final Set<String> taskIdSet = new HashSet<>();
    /** 当前节点id   */
    private String nodeId;
    /** 运行标志, 控制工作线程的启停，防止重复启动，做到优雅关闭  */
    private final AtomicBoolean running = new AtomicBoolean(false);
    /** 加载数据执行器   */
    private final ScheduledExecutorService loadExecutor = Executors.newSingleThreadScheduledExecutor();
    /** 运行工作执行器   */
    private final ScheduledExecutorService runningExecutor = Executors.newSingleThreadScheduledExecutor();
    /** 健康检查执行器   */
    private final ScheduledExecutorService healthExecutor = Executors.newSingleThreadScheduledExecutor();
    /** 删除已执行数据执行器   */
    private final ScheduledExecutorService removeExecutor = Executors.newSingleThreadScheduledExecutor();
    /** 工作线程池，异步执行任务业务逻辑   */
    private final PlasticeneThreadExecutor workExecutor = new PlasticeneThreadExecutor(
            Runtime.getRuntime().availableProcessors() + 1,
            Runtime.getRuntime().availableProcessors() * 5,
            1000,
            "delay-consumer-"
            );


    /**
     * 添加延迟任务的入口，这是分布式延时队列暴露给业务方使用的；
     * 执行时间在拉取数据周期内，直接放入当前节点延时队列执行，
     * 这样能让任务准时触发，但是存储任务之后被拉取进行分片，可能分到其他节点重复执行
     * eg: 任务A在5分钟之后执行，但是拉取数据到本地队列周期是10分钟，如果等到周期去拉取，那么任务A就没办法准时执行了
     * @param delayTask 延迟任务
     */
    public void addTask(DelayTask delayTask) {
        long executeTime = delayTask.getExecuteTime();
        long afterTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(delayProperties.getPullPeriod());
        if (afterTime > executeTime) {
            offerTask(delayTask);
        }
        taskStorage.addTask(delayTask);
    }

    /**
     * 服务启动，进行初始化相关操作
     */
    @Override
    public void afterPropertiesSet() {
        // 获取业务类型
        taskExecutors.forEach(taskExecutor -> this.queueNameSet.add(taskExecutor.queueName()));
        // 1.注册节点，建立心跳机制
        this.nodeId = coordinator.registerNode();
        coordinator.heartBeat(this.nodeId);

        // 2.启动工作线程
        running.set(true);
        runningExecutor.submit(()-> {
            while (running.get()) {
                try {
                    // 根据不同业务队列名称扫描延迟队列
                    for (Map.Entry<String, DelayQueue<DelayTask>> entry : delayMap.entrySet()) {
                        DelayQueue<DelayTask> delayQueue = entry.getValue();
                        DelayTask task = delayQueue.poll(1, TimeUnit.SECONDS);
                        executeTask(task);
                    }
                    // 睡眠1s
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    running.set(false);
                    Thread.currentThread().interrupt();
                    logger.error("delay task error:", e);
                }
            }
        });
        // 3.数据加载：每隔一定周期拉取一次数据 如每隔10分钟拉取，拉取数据是执行时间【0，当前时间+10m】
        loadExecutor.scheduleAtFixedRate(this::loadTask, delayProperties.getPullInitialDelay(),
                delayProperties.getPullPeriod(), TimeUnit.SECONDS);
        // 4.启动集群节点监控检查
        healthExecutor.scheduleWithFixedDelay(
                () -> coordinator.checkClusterHealth(this::moveOfflineNodeTask),
                delayProperties.getHealthInitialDelay(), delayProperties.getHealthPeriod(), TimeUnit.SECONDS);
        // 5.删除执行过的记录
        long endTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(delayProperties.getRemovePeriod());
        removeExecutor.scheduleAtFixedRate(() -> taskStorage.removeExecutedTask(0L, endTime),
                delayProperties.getRemoveInitialDelay(), delayProperties.getRemovePeriod(), TimeUnit.SECONDS);
    }

    @Override
    public void destroy() {
        // 关闭时清理
        logger.info("distributeDelayQueue destroy");
        running.set(false);
        coordinator.unRegisterNode(this.nodeId);
        healthExecutor.shutdown();
        loadExecutor.shutdown();
        runningExecutor.shutdown();
        workExecutor.shutdown();
        removeExecutor.shutdown();
        delayMap.clear();
        taskIdSet.clear();
        queueNameSet.clear();
    }

    private void executeTask(DelayTask task) {
        if (task == null) {
            return;
        }
        // 真正执行延时任务逻辑
        workExecutor.submit(() -> {
            // 先删除本地taskId
            taskIdSet.remove(task.queueTaskId());
            // 因可能重复分配导致同一任务存在于不同节点上，分布式锁控制一个任务同一时间只能在一个节点上执行
            RLock lock = redissonClient.getLock(DelayConstant.DELAY_EXECUTING_KEY_PREFIX + task.queueTaskId());
            try {
                boolean isLock = lock.tryLock();
                // 没有获得锁直接返回
                if (!isLock) {
                    return;
                }
                taskStorage.removeTask(task);
                // 判断是否执行过，防止重复执行
                boolean executed = taskStorage.isExecuted(task);
                if (executed) {
                    logger.info("task executed, queueTaskId:{}", task.queueTaskId());
                    return;
                }
                // 先记录执行过标识
                taskStorage.addExecutedTask(task);
                // 匹配对应业务的延时任务处理器
                taskExecutors.forEach(executor -> {
                    if (Objects.equals(task.getQueueName(), executor.queueName())) {
                        // 执行延时任务业务处理逻辑
                        executor.run(task);
                    }
                });
                logger.info("task execute success, queueTaskId:{}  ", task.queueTaskId());
            } catch (Exception e) {
                logger.error(" task execute error queueTaskId: {}", task.queueTaskId(), e);
            } finally {
                lock.unlock();
            }
        });

    }


    /**
     * 将下线的节点的任务分配到当前节点来
     * @param originNodes  集群元节点
     * @param deadNodes  下线节点
     */
    private void moveOfflineNodeTask(List<String> originNodes, List<String> deadNodes) {
        if (!running.get()) {
            return;
        }
        if (CollUtil.isEmpty(originNodes) || CollUtil.isEmpty(deadNodes)) {
            return;
        }
        long end = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(delayProperties.getPullPeriod());
        deadNodes.forEach(deadNode -> {
            queueNameSet.forEach(queueName -> {
                List<DelayTask> tasks = taskStorage.listTask(queueName, 0L, end);
                tasks.forEach(task-> {
                    int hash = Math.abs(task.getTaskId().hashCode());
                    int index = hash % originNodes.size();
                    // 判断当前任务之前是否分配到了下线节点上
                    if (Objects.equals(originNodes.get(index), deadNode)) {
                        offerTask(task);
                    }
                });
            });
        });
    }


    /**
     * 拉取数据
     * 如加载执行时间为【0, 当前时间+10分钟】内的数据
     * 每10分钟拉取一次，下一次拉取之前正常情况上一次拉取的数据已经处理完成了，
     * 所以一般情况任务存储和本地延时队列内存里面都不会堆积过多数据
     */
    private void loadTask() {
        if (!running.get()) {
            return;
        }
        List<String> activeNodes = coordinator.getActiveNodes();
        long end = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(delayProperties.getPullPeriod());
        queueNameSet.forEach(queueName -> {
            List<DelayTask> tasks = taskStorage.listTask(queueName, 0L, end);
            tasks.forEach(task-> {
                // 判断任务是否应该分配到此节点
                if (canProcess(task, activeNodes)) {
                    this.offerTask(task);
                }
            });
        });

    }

    /**
     * 判断任务是否可以分配当前节点
     */
    private Boolean canProcess(DelayTask task, List<String> activeNodes) {
        if (!running.get()) {
            return false;
        }
        if (taskIdSet.contains(task.queueTaskId())) {
            return false;
        }
        if (CollUtil.isEmpty(activeNodes)) {
            return false;
        }
        int hash = Math.abs(task.getTaskId().hashCode());
        int index = hash % activeNodes.size();
        return activeNodes.get(index).equals(this.nodeId);
    }

    /**
     * 添加延时任务到本地延时队列
     */
    private void offerTask(DelayTask task) {
        String queueName = task.getQueueName();
        DelayQueue<DelayTask> delayQueue = delayMap.computeIfAbsent(
                queueName,
                k -> new DelayQueue<>()
        );
        delayQueue.offer(task);
        // 记录当前节点已经加载了这个业务任务
        taskIdSet.add(task.queueTaskId());
    }
}

