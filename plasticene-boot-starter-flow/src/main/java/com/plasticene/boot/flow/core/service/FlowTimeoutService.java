package com.plasticene.boot.flow.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.entity.FlowTimeoutNotice;

import java.time.LocalDateTime;

/**
 * 在实例锁内执行超时审批与提醒调度，并通过持久化消息隔离渠道故障。
 * @author ZFJ
 * @since 2026-10-22
 */
public interface FlowTimeoutService extends IService<FlowTimeoutNotice> {

    /**
     * 处理超时的待办任务：任务超时或者任务下次提醒时间到期
     */
    void handleDueTask(FlowTask dueTask, LocalDateTime now);

    /**
     * 发送超时提醒通知
     */
    void sendNotification(Long noticeId, LocalDateTime now);
}
