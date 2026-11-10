package com.plasticene.boot.flow.core.service.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.dao.FlowTimeoutNoticeDAO;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.entity.FlowTimeoutNotice;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowTimeoutNoticeEnums;
import com.plasticene.boot.flow.core.event.FlowEventPublisher;
import com.plasticene.boot.flow.core.event.FlowEventType;
import com.plasticene.boot.flow.core.event.FlowTaskAction;
import com.plasticene.boot.flow.core.event.FlowTaskCancelReason;
import com.plasticene.boot.flow.core.event.FlowTaskEvent;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 维护任务状态、生命周期事件及提醒记录。
 * @author ZFJ
 * @since 2026-11-10
 */
@Service
public class FlowTaskLifecycle {
    @Resource
    private FlowTaskDAO flowTaskDAO;

    @Resource
    @Lazy
    private FlowRuntimeService flowRuntimeService;

    @Resource
    private FlowTimeoutNoticeDAO flowTimeoutNoticeDAO;

    @Resource
    private FlowEventPublisher flowEventPublisher;

    @Resource
    private FlowTaskQueries flowTaskQuery;

    public void delInstanceRunningTask(Long instanceId) {
        List<FlowTask> cancelled = flowTaskQuery.runningApprovalTasks(instanceId);
        LambdaUpdateWrapper<FlowTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FlowTask::getInstanceId, instanceId);
        updateWrapper.eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode());
        updateWrapper.isNull(FlowTask::getReturnTaskId);
        updateWrapper.set(FlowTask::getIsDelete, CommonConstant.IS_DEL);
        flowTaskDAO.update(updateWrapper);
        publishCancelledTasks(cancelled, FlowTaskCancelReason.INSTANCE_REJECTED);
    }

    public void cancelInstanceRunningTask(Long instanceId) {
        List<FlowTask> cancelled = flowTaskQuery.runningApprovalTasks(instanceId);
        FlowTask task = new FlowTask();
        task.setStatus(FlowTaskStatusEnum.CANCEL.getCode());
        task.setEndTime(LocalDateTime.now());
        task.setComment("发起人取消流程");
        LambdaUpdateWrapper<FlowTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FlowTask::getInstanceId, instanceId);
        updateWrapper.eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode());
        updateWrapper.eq(FlowTask::getIsDelete, CommonConstant.IS_NOT_DEL);
        updateWrapper.isNull(FlowTask::getReturnTaskId);
        flowTaskDAO.update(task, updateWrapper);
        publishCancelledTasks(cancelled, FlowTaskCancelReason.INSTANCE_CANCELLED);
    }

    public void publishCreatedTask(FlowInstance instance, FlowTask task) {
        publishTask(instance, task, FlowEventType.TASK_CREATED, FlowTaskStatusEnum.RUNNING.getCode(), null, null);
    }

    public void publishTask(FlowInstance instance, FlowTask task, FlowEventType type, Integer status,
                             FlowTaskAction action, FlowTaskCancelReason reason) {
        flowEventPublisher.publish(FlowTaskEvent.of(instance, task, type, status, action, reason));
    }

    public void publishCancelledTasks(List<FlowTask> tasks, FlowTaskCancelReason reason) {
        if (tasks.isEmpty()) {
            return;
        }
        FlowInstance instance = flowRuntimeService.getById(tasks.getFirst().getInstanceId());
        for (FlowTask task : tasks) {
            publishTask(instance, task, FlowEventType.TASK_CANCELLED,
                    FlowTaskStatusEnum.CANCEL.getCode(), null, reason);
        }
    }

    public void cancelPendingReminders(List<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return;
        }
        flowTimeoutNoticeDAO.update(new LambdaUpdateWrapper<FlowTimeoutNotice>().in(FlowTimeoutNotice::getTaskId, taskIds)
                .eq(FlowTimeoutNotice::getEventType, FlowTimeoutNoticeEnums.EventType.REMINDER.getCode())
                .eq(FlowTimeoutNotice::getState, FlowTimeoutNoticeEnums.State.PENDING.getCode())
                .set(FlowTimeoutNotice::getState, FlowTimeoutNoticeEnums.State.CANCELLED.getCode()));
    }

    public void completeTask(FlowTask source, FlowInstance instance,
                              LoginUser loginUser,
                              String comment,
                              FlowTaskStatusEnum statusEnum) {
        Long taskId = source.getId();
        FlowTask task = new FlowTask();
        task.setId(taskId);
        task.setStatus(statusEnum.getCode());
        task.setComment(comment);
        task.setEndTime(LocalDateTime.now());
        LambdaUpdateWrapper<FlowTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FlowTask::getId, taskId).eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode());
        updateWrapper.eq(FlowTask::getIsDelete, CommonConstant.IS_NOT_DEL);
        updateWrapper.isNull(FlowTask::getReturnTaskId);
        updateWrapper.eq(FlowTask::getOrgId, loginUser.getOrgId());
        updateWrapper.eq(FlowTask::getAssignee, loginUser.getId());
        updateWrapper.eq(FlowTask::getNodeType, FlowNodeEnum.Type.APPROVE.getCode());
        int update = flowTaskDAO.update(task, updateWrapper);
        if (update != 1) {
            throw new BizException("任务已经处理过，请勿重复处理");
        }
        FlowTaskAction action = switch (statusEnum) {
            case APPROVE -> FlowTaskAction.APPROVE;
            case REJECT -> FlowTaskAction.REJECT;
            case RETURNED -> FlowTaskAction.RETURN;
            default -> throw new IllegalArgumentException("不支持的任务完成状态");
        };
        publishTask(instance, source, FlowEventType.TASK_COMPLETED, statusEnum.getCode(), action, null);
    }
}
