package com.plasticene.boot.flow.core.impl.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.utils.PtcBeanUtils;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowInstance;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTask;
import com.plasticene.boot.flow.core.model.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.api.event.FlowTaskCancelReason;
import com.plasticene.boot.flow.core.impl.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.model.FlowNode;
import com.plasticene.boot.flow.core.api.param.FlowTaskReturnParam;
import com.plasticene.boot.flow.core.api.vo.FlowTaskReturnTargetVO;
import com.plasticene.boot.flow.core.impl.tree.FlowParser;
import com.plasticene.boot.flow.core.impl.tree.FlowReturnPath;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 处理流程退回及受影响任务和网关记录。
 * @author ZFJ
 * @since 2026-11-10
 */
@Service
public class FlowTaskReturn {
    @Resource
    private FlowTaskDAO flowTaskDAO;

    @Resource
    @Lazy
    private ProcessExecutor processExecutor;

    @Resource
    private FlowTaskQueries flowTaskQuery;

    @Resource
    private FlowTaskLifecycle flowTaskLifecycle;

    @Resource
    private FlowTaskExecution flowTaskExecution;

    @Resource
    private FlowTaskAddSignOperations flowTaskAddSign;

    public List<FlowTaskReturnTargetVO> listReturnTargets(FlowTask task, FlowInstance instance) {
        flowTaskAddSign.requireOriginalNodeTask(task);
        FlowNode root = flowTaskExecution.getReturnModel(instance);
        flowTaskExecution.requireNodeOperation(root, task, FlowNode::getAllowReturn, "退回");
        // 获取可退回的目标节点
        return FlowReturnPath.targets(root, task.getNodeKey(), flowTaskQuery.listTaskByInstanceId(instance.getId())).stream()
                .map(node -> new FlowTaskReturnTargetVO(node.getKey(), node.getName())).toList();
    }

    public void returnTask(FlowTaskReturnParam param, FlowTask task, FlowInstance instance, LoginUser user, String comment) {
        flowTaskAddSign.requireOriginalNodeTask(task);
        FlowNode root = flowTaskExecution.getReturnModel(instance);
        flowTaskExecution.requireNodeOperation(root, task, FlowNode::getAllowReturn, "退回");
        List<FlowTask> history = flowTaskQuery.listTaskByInstanceId(instance.getId());
        FlowNode target = FlowReturnPath.targets(root, task.getNodeKey(), history).stream()
                .filter(node -> Objects.equals(node.getKey(), param.getTargetNodeKey())).findFirst()
                .orElseThrow(() -> new BizException("退回目标不是有效的已完成前序人工审批节点，请刷新后重试"));
        FlowNode current = FlowParser.findNodeByKey(root, task.getNodeKey());
        Set<String> affectedKeys = FlowReturnPath.reachable(target);
        List<FlowTask> affected = history.stream().filter(FlowReturnPath::isActive)
                .filter(item -> affectedKeys.contains(item.getNodeKey())).toList();
        Long returnTaskId = task.getId();
        LocalDateTime now = LocalDateTime.now();
        flowTaskLifecycle.completeTask(task, instance, user, comment, FlowTaskStatusEnum.RETURNED);
        flowTaskDAO.update(new LambdaUpdateWrapper<FlowTask>()
                .eq(FlowTask::getId, returnTaskId).set(FlowTask::getReturnTargetNodeKey, target.getKey()));
        flowTaskAddSign.cancelRunningAddSigns(instance.getId(), affectedKeys, FlowTaskCancelReason.RETURN);
        invalidateTasks(affected, returnTaskId, now);
        // 包含目标的并行网关仍保留其他分支的执行结果，只撤销重走分支的汇合贡献。
        FlowReturnPath.enclosingGateways(target, current).forEach((gatewayKey, completed) -> {
            FlowTask gateway = history.stream().filter(FlowReturnPath::isActive)
                    .filter(item -> Objects.equals(item.getNodeKey(), gatewayKey)).findFirst()
                    .orElseThrow(() -> new BizException("并行网关执行记录不存在"));
            int count = gateway.getCompletedBranch() == null ? 0 : gateway.getCompletedBranch();
            if (completed && count < 1) {
                throw new BizException("并行网关完成计数异常");
            }
            // 先复制再失效，历史网关的计数不可被后续审批覆盖。
            FlowTask next = PtcBeanUtils.copy(gateway, FlowTask.class);
            next.setId(null);
            next.setCreator(null);
            next.setUpdater(null);
            next.setCreateTime(null);
            next.setUpdateTime(null);
            next.setCompletedBranch(count - (completed ? 1 : 0));
            invalidateTasks(List.of(gateway), returnTaskId, now);
            flowTaskDAO.insert(next);
        });
        processExecutor.executeNode(instance, target);
    }

    private void invalidateTasks(List<FlowTask> tasks, Long returnTaskId, LocalDateTime now) {
        if (tasks.isEmpty()) {
            return;
        }
        List<Long> ids = tasks.stream().map(FlowTask::getId).toList();
        List<FlowTask> cancelled = flowTaskQuery.runningApprovalTasks(tasks.getFirst().getInstanceId()).stream()
                .filter(task -> ids.contains(task.getId())).toList();
        flowTaskDAO.update(new LambdaUpdateWrapper<FlowTask>().in(FlowTask::getId, ids)
                .isNull(FlowTask::getReturnTaskId)
                .set(FlowTask::getReturnTaskId, returnTaskId)
                .set(FlowTask::getNextRemindTime, null));
        flowTaskDAO.update(new LambdaUpdateWrapper<FlowTask>().in(FlowTask::getId, ids)
                .in(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode(), FlowTaskStatusEnum.WAITING_ADD_SIGN.getCode())
                .set(FlowTask::getStatus, FlowTaskStatusEnum.CANCEL.getCode())
                .set(FlowTask::getEndTime, now).set(FlowTask::getComment, "因流程退回取消待办"));
        flowTaskLifecycle.publishCancelledTasks(cancelled, FlowTaskCancelReason.RETURN);
        flowTaskLifecycle.cancelPendingReminders(ids);
    }
}
