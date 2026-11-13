package com.plasticene.boot.flow.core.impl.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.utils.PtcBeanUtils;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowTaskAddSignDAO;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowInstance;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTask;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTaskAddSign;
import com.plasticene.boot.flow.core.model.enums.FlowAddSignEnum;
import com.plasticene.boot.flow.core.model.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.model.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.api.event.FlowEventType;
import com.plasticene.boot.flow.core.api.event.FlowTaskCancelReason;
import com.plasticene.boot.flow.core.model.FlowNode;
import com.plasticene.boot.flow.core.api.param.FlowTaskAddSignParam;
import com.plasticene.boot.flow.core.impl.tree.FlowReturnPath;
import com.plasticene.boot.flow.core.api.spi.FlowOrganizationProvider;
import com.plasticene.boot.flow.core.impl.service.instance.FlowRuntimeService;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * 管理加签批次、成员审批及原任务恢复。
 * @author ZFJ
 * @since 2026-11-10
 */
@Service
public class FlowTaskAddSignOperations {
    @Resource
    private FlowTaskDAO flowTaskDAO;

    @Resource
    @Lazy
    private FlowRuntimeService flowRuntimeService;

    @Resource
    private FlowOrganizationProvider flowOrganizationProvider;

    @Resource
    private FlowTaskAddSignDAO flowTaskAddSignDAO;

    @Resource
    private FlowTaskQueries flowTaskQuery;

    @Resource
    private FlowTaskCreation flowTaskCreation;

    @Resource
    private FlowTaskLifecycle flowTaskLifecycle;

    @Resource
    private FlowTaskExecution flowTaskExecution;

    public void addSign(FlowTaskAddSignParam param, FlowTask task, FlowInstance instance, LoginUser user) {
        requireOriginalNodeTask(task);
        flowTaskExecution.requireNodeOperation(flowTaskExecution.getReturnModel(instance), task, FlowNode::getAllowAddSign, "加签");
        validateAddSign(param, task, user);
        LocalDateTime now = LocalDateTime.now();
        FlowTaskAddSign batch = new FlowTaskAddSign();
        batch.setOrgId(task.getOrgId());
        batch.setInstanceId(task.getInstanceId());
        batch.setNodeKey(task.getNodeKey());
        batch.setSourceTaskId(task.getId());
        batch.setInitiatorId(user.getId());
        batch.setSignType(param.getType());
        batch.setApproveMode(param.getType() == FlowAddSignEnum.Type.PARALLEL
                ? task.getApproveMode() : param.getApproveMode());
        batch.setAssigneeIds(new ArrayList<>(param.getAssigneeIds()));
        batch.setComment(param.getComment().trim());
        batch.setCreateTime(now);
        batch.setUpdateTime(now);
        batch.setState(param.getType() == FlowAddSignEnum.Type.PARALLEL
                ? FlowAddSignEnum.State.APPLIED : FlowAddSignEnum.State.RUNNING);
        if (param.getType() == FlowAddSignEnum.Type.PARALLEL) {
            batch.setEndTime(now);
        } else {
            batch.setPausedAt(now);
        }
        flowTaskAddSignDAO.insert(batch);
        if (param.getType() == FlowAddSignEnum.Type.PARALLEL) {
            if (Objects.equals(task.getApproveMode(), FlowNodeEnum.ApproveMode.ORDER.getCode())) {
                List<Long> queue = new ArrayList<>(task.getAssigneeList());
                queue.addAll(queue.indexOf(task.getAssignee()) + 1, batch.getAssigneeIds());
                FlowTask update = new FlowTask();
                update.setAssigneeList(queue);
                flowTaskDAO.update(update, new LambdaUpdateWrapper<FlowTask>().eq(FlowTask::getId, task.getId()));
            } else {
                for (Long id : batch.getAssigneeIds()) {
                    createAddSignTask(task, batch, id);
                }
            }
            return;
        }
        int updated = flowTaskDAO.update(new LambdaUpdateWrapper<FlowTask>().eq(FlowTask::getId, task.getId())
                .eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode())
                .set(FlowTask::getStatus, FlowTaskStatusEnum.WAITING_ADD_SIGN.getCode()));
        if (updated != 1) {
            throw new BizException("任务已发生变化，请刷新后重试");
        }
        flowTaskLifecycle.publishTask(instance, task, FlowEventType.TASK_CANCELLED, FlowTaskStatusEnum.WAITING_ADD_SIGN.getCode(),
                null, FlowTaskCancelReason.ADD_SIGN_WAITING);
        flowTaskLifecycle.cancelPendingReminders(List.of(task.getId()));
        List<Long> starters = Objects.equals(batch.getApproveMode(), FlowNodeEnum.ApproveMode.ORDER.getCode())
                ? List.of(batch.getAssigneeIds().getFirst()) : batch.getAssigneeIds();
        for (Long id : starters) {
            createAddSignTask(task, batch, id);
        }
    }

    public boolean isAddSignGroupTask(FlowTask task) {
        FlowTaskAddSign batch = getAddSignBatch(task);
        return batch != null && batch.getSignType() != FlowAddSignEnum.Type.PARALLEL;
    }

    public FlowTaskAddSign getAddSignBatch(FlowTask task) {
        if (task == null || task.getAddSignId() == null) {
            return null;
        }
        FlowTaskAddSign batch = flowTaskAddSignDAO.selectById(task.getAddSignId());
        if (batch == null || !Objects.equals(batch.getOrgId(), task.getOrgId())
                || !Objects.equals(batch.getInstanceId(), task.getInstanceId())) {
            throw new BizException("加签任务关联异常，请刷新后重试");
        }
        return batch;
    }

    public void requireOriginalNodeTask(FlowTask task) {
        if (isAddSignGroupTask(task)) {
            throw new BizException("前后加签任务仅支持通过或拒绝");
        }
    }

    /** 后加签完成时返回待结算的原任务，由审批组件继续节点流转。 */
    public Optional<FlowTask> approveAddSignTask(FlowTask task, LoginUser user, String comment, FlowInstance instance) {
        FlowTaskAddSign batch = getAddSignBatch(task);
        FlowTask source = flowTaskDAO.selectById(batch.getSourceTaskId());
        if (batch.getState() != FlowAddSignEnum.State.RUNNING || source == null
                || !Objects.equals(source.getStatus(), FlowTaskStatusEnum.WAITING_ADD_SIGN.getCode())
                || !FlowReturnPath.isActive(source) || !Objects.equals(source.getIsDelete(), CommonConstant.IS_NOT_DEL)) {
            throw new BizException("加签已结束，请刷新后查看");
        }
        flowTaskLifecycle.completeTask(task, instance, user, comment, FlowTaskStatusEnum.APPROVE);
        if (Objects.equals(batch.getApproveMode(), FlowNodeEnum.ApproveMode.ALL.getCode())) {
            List<FlowTask> members = flowTaskDAO.selectList(new LambdaQueryWrapper<FlowTask>()
                    .eq(FlowTask::getAddSignId, batch.getId()).eq(FlowTask::getIsDelete, CommonConstant.IS_NOT_DEL));
            if (members.stream().anyMatch(member -> !Objects.equals(member.getStatus(), FlowTaskStatusEnum.APPROVE.getCode()))) {
                return Optional.empty();
            }
        } else if (Objects.equals(batch.getApproveMode(), FlowNodeEnum.ApproveMode.ANY.getCode())) {
            cancelAddSignMembers(batch, FlowTaskCancelReason.NODE_COMPLETED);
        } else {
            int index = batch.getAssigneeIds().indexOf(task.getAssignee());
            if (index < 0) {
                throw new BizException("加签顺序数据异常");
            }
            if (index + 1 < batch.getAssigneeIds().size()) {
                createAddSignTask(source, batch, batch.getAssigneeIds().get(index + 1));
                return Optional.empty();
            }
        }
        finishAddSign(batch, FlowAddSignEnum.State.PASSED);
        Duration paused = Duration.between(batch.getPausedAt(), LocalDateTime.now());
        LambdaUpdateWrapper<FlowTask> resume = new LambdaUpdateWrapper<FlowTask>()
                .eq(FlowTask::getId, source.getId()).eq(FlowTask::getStatus, FlowTaskStatusEnum.WAITING_ADD_SIGN.getCode())
                .set(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode());
        if (batch.getSignType() == FlowAddSignEnum.Type.BEFORE) {
            resume.set(FlowTask::getDeadline, source.getDeadline() == null ? null : source.getDeadline().plus(paused));
            resume.set(FlowTask::getNextRemindTime, source.getNextRemindTime() == null ? null : source.getNextRemindTime().plus(paused));
        }
        if (flowTaskDAO.update(resume) != 1) {
            throw new BizException("原审批任务已发生变化，请刷新后重试");
        }
        if (batch.getSignType() == FlowAddSignEnum.Type.BEFORE) {
            flowTaskLifecycle.publishCreatedTask(instance, source);
        }
        if (batch.getSignType() == FlowAddSignEnum.Type.AFTER) {
            LoginUser owner = new LoginUser();
            owner.setId(source.getAssignee());
            owner.setOrgId(source.getOrgId());
            flowTaskLifecycle.completeTask(source, instance, owner, batch.getComment(), FlowTaskStatusEnum.APPROVE);
            return Optional.of(source);
        }
        return Optional.empty();
    }

    public void finishAddSign(FlowTaskAddSign batch, FlowAddSignEnum.State state) {
        batch.setState(state);
        batch.setEndTime(LocalDateTime.now());
        flowTaskAddSignDAO.updateById(batch);
    }

    public Long findParallelAddSignId(FlowTask task, Long assignee) {
        Set<Long> sources = flowTaskQuery.listTasksByInstanceIdAndNodeKey(task.getInstanceId(), task.getNodeKey()).stream()
                .map(FlowTask::getId).collect(Collectors.toSet());
        return flowTaskQuery.listAddSigns(task.getInstanceId()).stream()
                .filter(batch -> batch.getSignType() == FlowAddSignEnum.Type.PARALLEL
                        && batch.getState() == FlowAddSignEnum.State.APPLIED
                        && Objects.equals(batch.getNodeKey(), task.getNodeKey())
                        && sources.contains(batch.getSourceTaskId()) && batch.getAssigneeIds().contains(assignee))
                .map(FlowTaskAddSign::getId).findFirst().orElse(null);
    }

    public void cancelRunningAddSigns(Long instanceId, Set<String> nodeKeys, FlowTaskCancelReason reason) {
        for (FlowTaskAddSign batch : flowTaskQuery.listAddSigns(instanceId)) {
            if (batch.getSignType() == FlowAddSignEnum.Type.PARALLEL
                    || batch.getState() != FlowAddSignEnum.State.RUNNING && batch.getState() != FlowAddSignEnum.State.REJECTED
                    || nodeKeys != null && !nodeKeys.contains(batch.getNodeKey())) {
                continue;
            }
            if (batch.getState() == FlowAddSignEnum.State.RUNNING) {
                cancelAddSignMembers(batch, reason);
                finishAddSign(batch, FlowAddSignEnum.State.CANCELLED);
            }
            flowTaskDAO.update(new LambdaUpdateWrapper<FlowTask>().eq(FlowTask::getId, batch.getSourceTaskId())
                    .eq(FlowTask::getStatus, FlowTaskStatusEnum.WAITING_ADD_SIGN.getCode())
                    .set(FlowTask::getStatus, FlowTaskStatusEnum.CANCEL.getCode())
                    .set(FlowTask::getEndTime, LocalDateTime.now()).set(FlowTask::getNextRemindTime, null));
            flowTaskLifecycle.cancelPendingReminders(List.of(batch.getSourceTaskId()));
        }
    }

    private void validateAddSign(FlowTaskAddSignParam param, FlowTask task, LoginUser user) {
        if (param.getType() == null || StrUtil.isBlank(param.getComment()) || param.getComment().trim().length() > 500) {
            throw new BizException("请选择加签类型并填写1至500字加签原因");
        }
        List<Long> ids = param.getAssigneeIds();
        if (CollUtil.isEmpty(ids) || ids.stream().anyMatch(id -> id == null || id <= 0)
                || ids.stream().distinct().count() != ids.size()) {
            throw new BizException("请选择不重复的有效加签人员");
        }
        if (param.getType() != FlowAddSignEnum.Type.PARALLEL
                && Arrays.stream(FlowNodeEnum.ApproveMode.values()).noneMatch(mode -> Objects.equals(mode.getCode(), param.getApproveMode()))) {
            throw new BizException("请选择加签审批方式");
        }
        Map<Long, String> activeUsers = flowOrganizationProvider.getActiveUserMap(user.getOrgId());
        if (ids.contains(user.getId()) || ids.stream().anyMatch(id -> !activeUsers.containsKey(id))) {
            throw new BizException("加签人员不能为本人，且须为本组织启用用户");
        }
        List<FlowTask> tasks = flowTaskQuery.listTasksByInstanceIdAndNodeKey(task.getInstanceId(), task.getNodeKey());
        Set<Long> existing = tasks.stream().map(FlowTask::getAssignee).filter(Objects::nonNull).collect(Collectors.toSet());
        tasks.stream().map(FlowTask::getAssigneeList).filter(Objects::nonNull).forEach(existing::addAll);
        if (ids.stream().anyMatch(existing::contains)) {
            throw new BizException("不能选择当前节点已有或排队中的审批人");
        }
        if (Objects.equals(task.getApproveMode(), FlowNodeEnum.ApproveMode.ORDER.getCode())
                && (task.getAssigneeList() == null || !task.getAssigneeList().contains(task.getAssignee()))) {
            throw new BizException("顺序审批人员数据异常，不能加签");
        }
        if (flowTaskQuery.listAddSigns(task.getInstanceId()).stream().anyMatch(batch -> Objects.equals(batch.getSourceTaskId(), task.getId())
                && batch.getState() == FlowAddSignEnum.State.RUNNING)) {
            throw new BizException("当前任务已在等待加签");
        }
    }

    private void createAddSignTask(FlowTask source, FlowTaskAddSign batch, Long assignee) {
        FlowTask task = PtcBeanUtils.copy(source, FlowTask.class);
        resetTaskForAssignee(task, assignee);
        task.setAddSignId(batch.getId());
        task.setApproveMode(batch.getApproveMode());
        task.setAssigneeList(batch.getSignType() == FlowAddSignEnum.Type.PARALLEL ? null : batch.getAssigneeIds());
        flowTaskCreation.initializeTimeout(task, source.getTimeoutRule());
        flowTaskDAO.insert(task);
        flowTaskLifecycle.publishCreatedTask(flowRuntimeService.getById(task.getInstanceId()), task);
    }

    private void resetTaskForAssignee(FlowTask task, Long assignee) {
        task.setId(null);
        task.setSourceTaskId(null);
        task.setTransferTo(null);
        task.setReturnTaskId(null);
        task.setReturnTargetNodeKey(null);
        task.setAssignee(assignee);
        task.setStatus(FlowTaskStatusEnum.RUNNING.getCode());
        task.setIsDelete(CommonConstant.IS_NOT_DEL);
        task.setStartTime(LocalDateTime.now());
        task.setEndTime(null);
        task.setComment(null);
        task.setCreator(null);
        task.setUpdater(null);
        task.setCreateTime(null);
        task.setUpdateTime(null);
    }

    private void cancelAddSignMembers(FlowTaskAddSign batch, FlowTaskCancelReason reason) {
        List<FlowTask> members = flowTaskDAO.selectList(new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getAddSignId, batch.getId()).eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode()));
        flowTaskDAO.update(new LambdaUpdateWrapper<FlowTask>().eq(FlowTask::getAddSignId, batch.getId())
                .eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode())
                .set(FlowTask::getStatus, FlowTaskStatusEnum.CANCEL.getCode())
                .set(FlowTask::getEndTime, LocalDateTime.now()).set(FlowTask::getNextRemindTime, null));
        flowTaskLifecycle.publishCancelledTasks(members, reason);
        flowTaskLifecycle.cancelPendingReminders(members.stream().map(FlowTask::getId).toList());
    }
}
