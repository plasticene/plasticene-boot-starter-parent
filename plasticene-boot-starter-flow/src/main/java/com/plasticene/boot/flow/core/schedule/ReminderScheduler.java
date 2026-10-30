package com.plasticene.boot.flow.core.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.plasticene.boot.flow.core.entity.FlowTimeoutNotice;
import com.plasticene.boot.flow.core.enums.FlowTimeoutNoticeEnums.State;
import com.plasticene.boot.flow.core.service.FlowTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

/**
 * @author ZFJ
 * @since 2026/10/23
 */
@Slf4j
@RequiredArgsConstructor
public class ReminderScheduler {
    private static final int SCAN_BATCH_SIZE = 200;

    private final FlowTimeoutService flowTimeoutService;


    /**
     * 发送通知提醒
     */
    @Scheduled(fixedDelayString = "${ptc.flow.timeout.notice-scan-interval-ms:30000}",
            scheduler = "flowTimeoutSchedulerExecutor")
    public void sendNotification() {
        LocalDateTime now = LocalDateTime.now();
        long startId = 0;
        while (true) {
            var batch = flowTimeoutService.list(new LambdaQueryWrapper<FlowTimeoutNotice>()
                    .select(FlowTimeoutNotice::getId, FlowTimeoutNotice::getOrgId)
                    .gt(FlowTimeoutNotice::getId, startId)
                    .eq(FlowTimeoutNotice::getState, State.PENDING.getCode())
                    .le(FlowTimeoutNotice::getRetryAt, now)
                    .orderByAsc(FlowTimeoutNotice::getId)
                    .last("LIMIT " + SCAN_BATCH_SIZE));
            for (FlowTimeoutNotice notice : batch) {
                try {
                    flowTimeoutService.sendNotification(notice.getId(), now);
                } catch (Exception failure) {
                    log.warn("超时通知处理失败 noticeId={}", notice.getId());
                }
                startId = notice.getId();
            }
            if (batch.size() < SCAN_BATCH_SIZE) {
                break;
            }
        }
    }
}
