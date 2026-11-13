package com.plasticene.boot.flow.core.impl.task;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowDefinition;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowInstance;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTask;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTaskAddSign;
import com.plasticene.boot.flow.core.model.enums.FlowAddSignEnum;
import com.plasticene.boot.flow.core.model.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.model.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.model.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.api.event.FlowTaskCancelReason;
import com.plasticene.boot.flow.core.model.FlowExtraSetting;
import com.plasticene.boot.flow.core.model.FlowNode;
import com.plasticene.boot.flow.core.impl.tree.FlowReturnPath;
import com.plasticene.boot.flow.core.impl.service.definition.FlowDefinitionService;
import com.plasticene.boot.flow.core.impl.service.instance.FlowRuntimeService;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 判断并执行审批结果撤回。
 * @author ZFJ
 * @since 2026-11-10
 */
@Service
public class FlowTaskWithdraw {
    @Resource
    private FlowTaskDAO flowTaskDAO;

    @Resource
    @Lazy
    private FlowRuntimeService flowRuntimeService;

    @Resource
    private FlowDefinitionService flowDefinitionService;

    @Resource
    private FlowTaskQueries flowTaskQuery;

    @Resource
    private FlowTaskLifecycle flowTaskLifecycle;

    @Resource
    private FlowTaskAddSignOperations flowTaskAddSign;

    /**
     * 校验操作者的撤回权限，撤回其已同意的审批结果并恢复相应待办。
     * @param taskId 任务 ID
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    @Transactional(rollbackFor = Exception.class)
    public void withdrawTask(Long taskId, LoginUser loginUser) {
        if (taskId == null) {
            throw new BizException("任务id不能为空");
        }
        FlowTask task = flowTaskDAO.selectById(taskId);
        if (task == null
                || !Objects.equals(task.getOrgId(), loginUser.getOrgId())
                || !Objects.equals(task.getAssignee(), loginUser.getId())) {
            throw new BizException("任务不存在或无权撤回");
        }
        FlowInstance instance = flowRuntimeService.selectInstanceForUpdate(task.getInstanceId());
        FlowDefinition definition = flowDefinitionService.getById(instance.getDefinitionId());
        List<FlowTask> instanceTasks = flowTaskQuery.listTaskByInstanceId(instance.getId());
        if (!canWithdrawTask(task, instance, definition, instanceTasks, loginUser)) {
            throw new BizException("当前审批结果不允许撤回");
        }

        boolean sameNode = Objects.equals(instance.getCurrentNodeKey(), task.getNodeKey());
        if (!sameNode || Objects.equals(task.getApproveMode(), FlowNodeEnum.ApproveMode.ORDER.getCode())) {
            List<FlowTask> cancelled = flowTaskQuery.runningApprovalTasks(instance.getId());
            LambdaUpdateWrapper<FlowTask> cancelRunningWrapper = new LambdaUpdateWrapper<>();
            cancelRunningWrapper.isNull(FlowTask::getReturnTaskId);
            cancelRunningWrapper.eq(FlowTask::getInstanceId, instance.getId())
                    .eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode())
                    .eq(FlowTask::getIsDelete, CommonConstant.IS_NOT_DEL)
                    .set(FlowTask::getIsDelete, CommonConstant.IS_DEL);
            flowTaskDAO.update(cancelRunningWrapper);
            flowTaskLifecycle.publishCancelledTasks(cancelled, FlowTaskCancelReason.WITHDRAW);
        }

        LambdaUpdateWrapper<FlowTask> withdrawWrapper = new LambdaUpdateWrapper<>();
        withdrawWrapper.isNull(FlowTask::getReturnTaskId);
        withdrawWrapper.eq(FlowTask::getId, taskId)
                .eq(FlowTask::getStatus, FlowTaskStatusEnum.APPROVE.getCode())
                .eq(FlowTask::getIsDelete, CommonConstant.IS_NOT_DEL)
                .set(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode())
                .set(FlowTask::getApproveType, FlowNodeEnum.ApproveType.MANUAL.getCode())
                .set(FlowTask::getEndTime, null)
                .set(FlowTask::getComment, null);
        int updated = flowTaskDAO.update(withdrawWrapper);
        if (updated != 1) {
            throw new BizException("审批结果已发生变化，请刷新后重试");
        }

        flowTaskLifecycle.publishCreatedTask(instance, task);
        FlowNode node = new FlowNode();
        node.setKey(task.getNodeKey());
        node.setName(task.getNodeName());
        flowRuntimeService.updateInstanceCurrentNode(instance.getId(), node);
    }

    /**
     * 根据操作者身份、流程配置和当前任务状态判断审批结果是否允许撤回。
     * @param task 任务快照
     * @param instance 流程实例快照
     * @param definition 流程发布定义快照
     * @param instanceTasks 实例内当前任务记录
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 当前操作者是否允许撤回该审批结果
     */
    public boolean canWithdrawTask(FlowTask task, FlowInstance instance,
                                   FlowDefinition definition, List<FlowTask> instanceTasks, LoginUser loginUser) {
        if (task == null || instance == null || definition == null || loginUser == null
                || !Objects.equals(instance.getStatus(), FlowInstanceStatusEnum.RUNNING.getCode())
                || !Objects.equals(instance.getOrgId(), loginUser.getOrgId())
                || !Objects.equals(task.getOrgId(), loginUser.getOrgId())
                || !Objects.equals(task.getAssignee(), loginUser.getId())
                || !Objects.equals(task.getNodeType(), FlowNodeEnum.Type.APPROVE.getCode())
                || !Objects.equals(task.getStatus(), FlowTaskStatusEnum.APPROVE.getCode())
                || !Objects.equals(task.getIsDelete(), CommonConstant.IS_NOT_DEL)
                || Boolean.TRUE.equals(task.getTimeoutHandled())
                || !FlowReturnPath.isActive(task)
                || task.getEndTime() == null) {
            return false;
        }
        List<FlowTaskAddSign> addSigns = flowTaskQuery.listAddSigns(task.getInstanceId());
        if (flowTaskAddSign.isAddSignGroupTask(task) || addSigns.stream()
                .anyMatch(batch -> Objects.equals(batch.getSourceTaskId(), task.getId())
                        && batch.getSignType() != FlowAddSignEnum.Type.PARALLEL)) {
            return false;
        }
        // 后续任务已发起加签也属于已执行操作，不能撤回并丢弃其队列或留下悬挂的等待任务。
        Set<Long> activeSources = instanceTasks.stream().filter(FlowReturnPath::isActive)
                .filter(item -> Objects.equals(item.getIsDelete(), CommonConstant.IS_NOT_DEL))
                .map(FlowTask::getId).collect(Collectors.toSet());
        if (addSigns.stream().anyMatch(batch -> activeSources.contains(batch.getSourceTaskId())
                && batch.getCreateTime() != null && !batch.getCreateTime().isBefore(task.getEndTime()))) {
            return false;
        }
        FlowExtraSetting setting = FlowExtraSetting.normalize(definition.getExtraSetting());
        if (!Boolean.TRUE.equals(setting.getAllowApproverWithdraw())) {
            return false;
        }
        boolean laterApprovalHandled = instanceTasks.stream()
                .filter(FlowReturnPath::isActive)
                .filter(item -> !Objects.equals(item.getId(), task.getId()))
                .filter(item -> Objects.equals(item.getNodeType(), FlowNodeEnum.Type.APPROVE.getCode()))
                .filter(item -> Objects.equals(item.getStatus(), FlowTaskStatusEnum.APPROVE.getCode())
                        || Objects.equals(item.getStatus(), FlowTaskStatusEnum.REJECT.getCode())
                        || Objects.equals(item.getStatus(), FlowTaskStatusEnum.TRANSFERRED.getCode()))
                .filter(item -> item.getEndTime() != null)
                .anyMatch(item -> item.getEndTime().isAfter(task.getEndTime())
                        || item.getEndTime().isEqual(task.getEndTime())
                        && item.getId() != null && task.getId() != null && item.getId() > task.getId());
        if (laterApprovalHandled) {
            return false;
        }
        List<FlowTask> runningTasks = instanceTasks.stream()
                .filter(FlowReturnPath::isActive)
                .filter(item -> Objects.equals(item.getStatus(), FlowTaskStatusEnum.RUNNING.getCode()))
                .filter(item -> Objects.equals(item.getIsDelete(), CommonConstant.IS_NOT_DEL))
                .toList();
        return CollUtil.isNotEmpty(runningTasks)
                && runningTasks.stream().allMatch(item -> Objects.equals(item.getNodeKey(),
                instance.getCurrentNodeKey()));
    }
}
