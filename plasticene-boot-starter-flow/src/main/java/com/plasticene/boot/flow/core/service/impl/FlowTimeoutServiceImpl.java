package com.plasticene.boot.flow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.flow.core.dao.FlowTimeoutNoticeDAO;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.entity.FlowTimeoutNotice;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.model.dto.FlowTimeoutRule;
import com.plasticene.boot.flow.core.provider.FlowTimeoutNotification;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import com.plasticene.boot.flow.core.service.FlowTimeoutService;
import jakarta.annotation.Resource;
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
@Service
public class FlowTimeoutServiceImpl extends ServiceImpl<FlowTimeoutNoticeDAO, FlowTimeoutNotice> implements FlowTimeoutService {
    @Resource
    private FlowTimeoutNoticeDAO flowTimeoutNoticeDAO;
    @Resource
    private FlowRuntimeService flowRuntimeService;
    @Resource
    private FlowTaskService flowTaskService;
    @Autowired(required = false)
    private FlowTimeoutNotification flowTimeoutNotification;

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void handleDueTask(FlowTask dueTask, LocalDateTime now) {
        if (dueTask == null) {
            return;
        }
        FlowInstance instance = flowRuntimeService.selectInstanceForUpdate(dueTask.getInstanceId());
        // 获得实例锁后重新读取最新已提交状态，不使用锁前候选数据。
        FlowTask task = flowTaskService.getById(dueTask.getId());
        // 已失效的待办或流程不再执行超时处理。
        if (isFinished(task) || task.getTimeoutRule() == null
                || instance == null || !Objects.equals(instance.getStatus(), FlowInstanceStatusEnum.RUNNING.getCode())
                || !Objects.equals(task.getOrgId(), instance.getOrgId())) {
            return;
        }
        // 任务超时
        if (task.getDeadline() != null && !task.getDeadline().isAfter(now)) {
            flowTaskService.update(new LambdaUpdateWrapper<FlowTask>().eq(FlowTask::getId, task.getId())
                    .eq(FlowTask::getStatus, 0).eq(FlowTask::getIsDelete, 0)
                    .set(FlowTask::getTimeoutHandled, true).set(FlowTask::getNextRemindTime, null));
            // 进行自动处理
            flowTaskService.executeTimeout(task);
            addTimeoutNotice(task, "RESULT", 0, now);
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
        addTimeoutNotice(task, "REMINDER", count + 1, now);
        LocalDateTime next = count + 1 >= rule.getMaxReminders() ? null
                : now.plusMinutes(FlowTimeoutRule.minutes(rule.getReminderInterval(), rule.getReminderIntervalUnit()));
        flowTaskService.update(new LambdaUpdateWrapper<FlowTask>().eq(FlowTask::getId, task.getId())
                .set(FlowTask::getRemindCount, count + 1)
                .set(FlowTask::getNextRemindTime, next));
    }

    private void addTimeoutNotice(FlowTask task, String type, int round, LocalDateTime now) {
        FlowTimeoutNotice notice = new FlowTimeoutNotice();
        notice.setOrgId(task.getOrgId());
        notice.setTaskId(task.getId());
        notice.setInstanceId(task.getInstanceId());
        notice.setEventType(type);
        notice.setRoundNumber(round);
        notice.setAttempts(0);
        notice.setCreateTime(now);
        notice.setState("PENDING");
        notice.setRetryAt(now);
        flowTimeoutNoticeDAO.insert(notice);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void sendNotification(Long noticeId, LocalDateTime now) {
        FlowTimeoutNotice candidate = flowTimeoutNoticeDAO.selectById(noticeId);
        if (candidate == null) {
            return;
        }
//        FlowInstance instance = flowRuntimeService.selectInstanceForUpdate(candidate.getInstanceId());
        FlowTimeoutNotice notice = flowTimeoutNoticeDAO.lock(noticeId, candidate.getOrgId());
        if (notice == null || !"PENDING".equals(notice.getState()) || notice.getRetryAt().isAfter(now)) {
            return;
        }
        FlowTask task = flowTaskService.getById(notice.getTaskId());
        if (task == null) {
            return;
        }
        if ("REMINDER".equals(notice.getEventType()) && (isFinished(task)
                || task.getDeadline() != null && !task.getDeadline().isAfter(now))) {
            notice.setState("CANCELLED");
        } else {
            int attempt = notice.getAttempts() + 1;
            try {
                FlowInstance instance = flowRuntimeService.getById(notice.getInstanceId());
                notice.setAttempts(attempt);
                var recipients = "RESULT".equals(notice.getEventType())
                        ? Stream.of(task.getAssignee(), instance.getUserId()).distinct().toList()
                        : List.of(task.getAssignee());
                flowTimeoutNotification.send(new FlowTimeoutNotification.Message(notice.getId(), notice.getOrgId(),
                        instance.getId(), task.getId(), task.getNodeName(), recipients, notice.getEventType(),
                        task.getTimeoutRule().getAction() == null ? null : task.getTimeoutRule().getAction().name(),
                        task.getDeadline(), notice.getCreateTime()));
                notice.setState("SENT");
                notice.setError(null);
                notice.setSentTime(now);
            } catch (Exception failure) {
                notice.setAttempts(attempt);
                notice.setState(attempt >= 3 ? "FAILED" : "PENDING");
                notice.setError("通知发送失败：" + failure.getClass().getSimpleName());
                notice.setRetryAt(now.plusMinutes(5));
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

    private boolean isFinished(FlowTask task) {
        return task == null
                || !Objects.equals(task.getStatus(), FlowTaskStatusEnum.RUNNING.getCode())
                || !Objects.equals(task.getIsDelete(), CommonConstant.IS_NOT_DEL)
                || !Objects.equals(task.getNodeType(), FlowNodeEnum.Type.APPROVE.getCode());
    }
}
