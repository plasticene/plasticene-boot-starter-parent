package com.plasticene.boot.flow.core.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.flow.core.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.service.FlowTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

/**
 * 独立调度到期任务扫描和通知投递扫描，通过实例锁支持多实例部署。
 * @author ZFJ
 * @since 2026-10-22
 */
@Slf4j
@RequiredArgsConstructor
public class FlowTimeoutScheduler {
    private static final int SCAN_BATCH_SIZE = 200;

    private final FlowTaskDAO flowTaskDAO;
    private final FlowTimeoutService flowTimeoutService;


    /**
     * 处理到期的待办任务：任务超时或者任务下次提醒时间到期
     */
    @Scheduled(fixedDelayString = "${ptc.flow.timeout.scan-interval-ms:30000}",
            scheduler = "flowTimeoutSchedulerExecutor")
    public void handleDueTasks() {
        LocalDateTime now = LocalDateTime.now();
        long startId = 0;
        while (true) {
            var batch = flowTaskDAO.selectList(new LambdaQueryWrapper<FlowTask>()
                    .select(FlowTask::getId, FlowTask::getOrgId, FlowTask::getInstanceId)
                    .gt(FlowTask::getId, startId)
                    .eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode())
                    .eq(FlowTask::getIsDelete, CommonConstant.IS_NOT_DEL)
                    .and(q -> q.le(FlowTask::getDeadline, now).or().le(FlowTask::getNextRemindTime, now))
                    .orderByAsc(FlowTask::getId)
                    .last("LIMIT " + SCAN_BATCH_SIZE));
            for (FlowTask task : batch) {
                try {
                    flowTimeoutService.handleDueTask(task, now);
                } catch (Exception failure) {
                    log.error("超时任务执行失败 taskId={}", task.getId(), failure);
                }
                startId = task.getId();
            }
            if (batch.size() < SCAN_BATCH_SIZE) {
                break;
            }
        }
    }
}
