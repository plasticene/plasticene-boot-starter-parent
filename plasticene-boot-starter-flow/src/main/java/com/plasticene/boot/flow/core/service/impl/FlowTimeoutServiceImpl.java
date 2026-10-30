package com.plasticene.boot.flow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.dao.FlowTimeoutNoticeDAO;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.entity.FlowTimeoutNotice;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowTimeoutNoticeEnums.EventType;
import com.plasticene.boot.flow.core.enums.FlowTimeoutNoticeEnums.State;
import com.plasticene.boot.flow.core.model.dto.FlowTimeoutRule;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;
import com.plasticene.boot.flow.core.provider.FlowTimeoutNotification;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import com.plasticene.boot.flow.core.service.FlowTimeoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author ZFJ
 * @since 2026/10/23
 */
@RequiredArgsConstructor
@Service
public class FlowTimeoutServiceImpl extends ServiceImpl<FlowTimeoutNoticeDAO, FlowTimeoutNotice> implements FlowTimeoutService {
    private final FlowTimeoutNoticeDAO flowTimeoutNoticeDAO;
    private final FlowRuntimeService flowRuntimeService;
    private final FlowTaskService flowTaskService;
    @Autowired(required = false)
    private FlowTimeoutNotification flowTimeoutNotification;

    private static final int MAX_SEND_ATTEMPTS = 3;
    private static final int RETRY_INTERVAL_MINUTES = 5;
    private static final int RESULT_NOTICE_ROUND = 0;

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void handleDueTask(FlowTask dueTask, LocalDateTime now) {
        if (dueTask == null) {
            return;
        }
        FlowInstance instance = flowRuntimeService.selectInstanceForUpdate(dueTask.getInstanceId());
        // 获得实例锁后重新读取最新任务状态，任务可能已经被处理了
        FlowTask task = flowTaskService.getById(dueTask.getId());
        // 已处理的任务或实例已结束，不再执行超时处理。
        if (isFinished(task) || !Objects.equals(instance.getStatus(), FlowInstanceStatusEnum.RUNNING.getCode())) {
            return;
        }
        // 任务超时
        if (task.getDeadline() != null && !task.getDeadline().isAfter(now)) {
            flowTaskService.update(new LambdaUpdateWrapper<FlowTask>()
                    .eq(FlowTask::getId, task.getId())
                    .eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode())
                    .eq(FlowTask::getIsDelete, CommonConstant.IS_NOT_DEL)
                    .set(FlowTask::getTimeoutHandled, true)
                    .set(FlowTask::getNextRemindTime, null));
            // 进行自动处理
            doExecuteTimeoutTask(task);
            // 添加结果通知记录
            addTimeoutNotice(task, EventType.RESULT, RESULT_NOTICE_ROUND, now);
            return;
        }
        if (task.getNextRemindTime() == null || task.getNextRemindTime().isAfter(now)) {
            return;
        }
        // 任务下次提醒时间到了
        FlowTimeoutRule rule = task.getTimeoutRule();
        int count = task.getRemindCount() == null ? 0 : task.getRemindCount();
        // 超过提醒次数，不再提醒
        if (count >= rule.getMaxReminders()) {
            return;
        }
        // 添加提醒通知记录
        addTimeoutNotice(task, EventType.REMINDER, count + 1, now);
        LocalDateTime next = count + 1 >= rule.getMaxReminders() ? null
                : now.plusMinutes(FlowTimeoutRule.minutes(rule.getReminderInterval(), rule.getReminderIntervalUnit()));
        flowTaskService.update(new LambdaUpdateWrapper<FlowTask>().eq(FlowTask::getId, task.getId())
                .set(FlowTask::getRemindCount, count + 1)
                .set(FlowTask::getNextRemindTime, next));
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void sendNotification(Long noticeId, LocalDateTime now) {
        // 获取通知记录并加锁
        FlowTimeoutNotice notice = flowTimeoutNoticeDAO.lock(noticeId);
        // 该通知记录已经被处理过了
        if (notice == null || !State.PENDING.getCode().equals(notice.getState()) || notice.getRetryAt().isAfter(now)) {
            return;
        }
        FlowTask task = flowTaskService.getById(notice.getTaskId());
        if (task == null) {
            return;
        }
        // 任务已处理或者已超时了，就不需要再发现提醒通知了
        if (EventType.REMINDER.getCode().equals(notice.getEventType()) &&
                (isFinished(task) || task.getDeadline() != null && !task.getDeadline().isAfter(now))) {
            notice.setState(State.CANCELLED.getCode());
        } else {
            int attempt = notice.getAttempts() + 1;
            try {
                FlowInstance instance = flowRuntimeService.getById(notice.getInstanceId());
                notice.setAttempts(attempt);
                var recipients = EventType.RESULT.getCode().equals(notice.getEventType())
                        ? Stream.of(task.getAssignee(), instance.getUserId()).distinct().toList()
                        : List.of(task.getAssignee());
                // 发送通知
                flowTimeoutNotification.send(new FlowTimeoutNotification.Message(notice.getId(), notice.getOrgId(),
                        instance.getId(), task.getId(), task.getNodeName(), recipients, notice.getEventType(),
                        task.getTimeoutRule().getAction() == null ? null : task.getTimeoutRule().getAction().name(),
                        task.getDeadline(), notice.getCreateTime()));
                notice.setState(State.SENT.getCode());
                notice.setError(null);
                notice.setSentTime(now);
            } catch (Exception failure) {
                notice.setAttempts(attempt);
                notice.setState(attempt >= MAX_SEND_ATTEMPTS ? State.FAILED.getCode() : State.PENDING.getCode());
                notice.setError("通知发送失败：" + failure.getClass().getSimpleName());
                notice.setRetryAt(now.plusMinutes(RETRY_INTERVAL_MINUTES));
            }
        }
        flowTimeoutNoticeDAO.update(new LambdaUpdateWrapper<FlowTimeoutNotice>()
                .eq(FlowTimeoutNotice::getId, noticeId).eq(FlowTimeoutNotice::getOrgId, notice.getOrgId())
                .set(FlowTimeoutNotice::getState, notice.getState())
                .set(FlowTimeoutNotice::getAttempts, notice.getAttempts())
                .set(FlowTimeoutNotice::getError, notice.getError())
                .set(FlowTimeoutNotice::getRetryAt, notice.getRetryAt())
                .set(FlowTimeoutNotice::getSentTime, notice.getSentTime()));
    }

    /**
     * 处理流转超时任务
     * 后台超时执行入口，调用方必须持有实例锁并校验待办截止时间
     */
    void doExecuteTimeoutTask(FlowTask task) {
        LoginUser owner = new LoginUser();
        owner.setId(task.getAssignee());
        owner.setOrgId(task.getOrgId());
        FlowTaskParam param = new FlowTaskParam();
        param.setTaskId(task.getId());
        boolean approve = task.getTimeoutRule().getAction() == FlowTimeoutRule.Action.APPROVE;
        param.setComment(approve ? "系统超时自动同意" : "系统超时自动拒绝");
        if (approve) {
            flowTaskService.approveTask(param, owner);
        } else {
            flowTaskService.rejectTask(param, owner);
        }

    }

    private void addTimeoutNotice(FlowTask task, EventType type, int round, LocalDateTime now) {
        FlowTimeoutNotice notice = new FlowTimeoutNotice();
        notice.setOrgId(task.getOrgId());
        notice.setTaskId(task.getId());
        notice.setInstanceId(task.getInstanceId());
        notice.setEventType(type.getCode());
        notice.setRoundNumber(round);
        notice.setAttempts(0);
        notice.setCreateTime(now);
        notice.setState(State.PENDING.getCode());
        notice.setRetryAt(now);
        flowTimeoutNoticeDAO.insert(notice);
    }

    private boolean isFinished(FlowTask task) {
        return task == null
                || !Objects.equals(task.getStatus(), FlowTaskStatusEnum.RUNNING.getCode())
                || !Objects.equals(task.getIsDelete(), CommonConstant.IS_NOT_DEL)
                || !Objects.equals(task.getNodeType(), FlowNodeEnum.Type.APPROVE.getCode());
    }
}
