package com.plasticene.boot.flow.core.service.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.enums.FlowAddSignEnum;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.event.FlowTaskCancelReason;
import com.plasticene.boot.flow.core.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.parser.FlowParser;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 处理审批结果收敛与后续节点流转。
 * @author ZFJ
 * @since 2026-11-10
 */
@Service
public class FlowTaskApproval {
    @Resource
    private FlowTaskDAO flowTaskDAO;

    @Resource
    @Lazy
    private FlowRuntimeService flowRuntimeService;

    @Resource
    @Lazy
    private ProcessExecutor processExecutor;

    @Resource
    private FlowDefinitionService flowDefinitionService;

    @Resource
    private FlowTaskQueries flowTaskQuery;

    @Resource
    private FlowTaskCreation flowTaskCreation;

    @Resource
    private FlowTaskLifecycle flowTaskLifecycle;

    @Resource
    private FlowTaskAddSignOperations flowTaskAddSign;

    public void approve(FlowTask task, FlowInstance instance, LoginUser user, String comment) {
        if (flowTaskAddSign.isAddSignGroupTask(task)) {
            flowTaskAddSign.approveAddSignTask(task, user, comment, instance)
                    .ifPresent(source -> settleNodeApproval(source, instance));
            return;
        }
        flowTaskLifecycle.completeTask(task, instance, user, comment, FlowTaskStatusEnum.APPROVE);
        settleNodeApproval(task, instance);
    }

    public void reject(Long taskId, String nodeKey, FlowInstance instance, LoginUser loginUser, String comment) {
        // 保留拒绝操作原有的锁内再次查询。
        FlowTask flowTask = flowTaskDAO.selectById(taskId);
        if (Objects.equals(flowTask.getIsDelete(), CommonConstant.IS_DEL)) {
            throw new BizException("当前节点已审批，不能再审");
        }
        flowTaskLifecycle.completeTask(flowTask, instance, loginUser, comment, FlowTaskStatusEnum.REJECT);
        if (flowTaskAddSign.isAddSignGroupTask(flowTask)) {
            flowTaskAddSign.finishAddSign(flowTaskAddSign.getAddSignBatch(flowTask), FlowAddSignEnum.State.REJECTED);
        }
        // 终止流程实例
        FlowDefinition flowDefinition = flowDefinitionService.getById(instance.getDefinitionId());
        FlowNode flowNode = flowDefinition.getModelNode();
        FlowParser.makeParentNode(flowNode);
        FlowNode currentNode = FlowParser.findNodeByKey(flowNode, nodeKey);
        flowRuntimeService.endInstance(instance, currentNode, FlowInstanceStatusEnum.REJECT);
    }

    private void settleNodeApproval(FlowTask flowTask, FlowInstance flowInstance) {
        Long taskId = flowTask.getId();
        Long instanceId = flowTask.getInstanceId();
        String nodeKey = flowTask.getNodeKey();
        Integer approveMode = flowTask.getApproveMode();
        // 会签
        if (Objects.equals(approveMode, FlowNodeEnum.ApproveMode.ALL.getCode())) {
            boolean existed = existUncompletedTask(instanceId, nodeKey);
            // 会签存在其他人未审批
            if (existed) {
                return;
            }
        }
        // 或签
        if (Objects.equals(approveMode, FlowNodeEnum.ApproveMode.ANY.getCode())) {
            flowTaskAddSign.cancelRunningAddSigns(instanceId, Set.of(nodeKey), FlowTaskCancelReason.NODE_COMPLETED);
            // 再次查询任务
            flowTask = flowTaskDAO.selectById(taskId);
            if (Objects.equals(flowTask.getIsDelete(), CommonConstant.IS_DEL)) {
                throw new BizException("当前节点已审批，不能再审");
            }
            List<FlowTask> cancelled = flowTaskQuery.runningApprovalTasks(instanceId, nodeKey);
            // 或签只结束当前审批节点，不能删除其他并行分支的待办。
            LambdaUpdateWrapper<FlowTask> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(FlowTask::getInstanceId, instanceId);
            updateWrapper.eq(FlowTask::getNodeKey, nodeKey);
            updateWrapper.eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode());
            updateWrapper.isNull(FlowTask::getReturnTaskId);
            updateWrapper.set(FlowTask::getIsDelete, CommonConstant.IS_DEL);
            flowTaskDAO.update(updateWrapper);
            flowTaskLifecycle.publishCancelledTasks(cancelled, FlowTaskCancelReason.NODE_COMPLETED);
        }
        // 顺序审批
        if (Objects.equals(approveMode, FlowNodeEnum.ApproveMode.ORDER.getCode())) {
            Long assignee = flowTask.getAssignee();
            List<Long> assigneeList = flowTask.getAssigneeList();
            int index = assigneeList.indexOf(assignee);
            if (index < assigneeList.size() - 1) {
                // 不是当前节点最后一个审批人，插入下一个审批人任务，不流入下个节点
                flowTask.setId(null);
                flowTask.setSourceTaskId(null);
                flowTask.setTransferTo(null);
                flowTask.setAssignee(assigneeList.get(index + 1));
                flowTask.setAddSignId(flowTaskAddSign.findParallelAddSignId(flowTask, flowTask.getAssignee()));
                flowTask.setStatus(FlowTaskStatusEnum.RUNNING.getCode());
                flowTask.setStartTime(LocalDateTime.now());
                flowTask.setEndTime(null);
                flowTask.setCreator(null);
                flowTask.setUpdater(null);
                flowTask.setCreateTime(null);
                flowTask.setUpdateTime(null);
                flowTask.setComment(null);
                flowTaskCreation.initializeTimeout(flowTask, flowTask.getTimeoutRule());
                flowTaskDAO.insert(flowTask);
                flowTaskLifecycle.publishCreatedTask(flowInstance, flowTask);
                return;
            }
        }

        // 根据流程模型查找当前节点并流转下一个节点
        FlowDefinition flowDefinition = flowDefinitionService.getById(flowInstance.getDefinitionId());
        FlowNode flowNode = flowDefinition.getModelNode();
        FlowParser.makeParentNode(flowNode);
        FlowNode currentNode = FlowParser.findNodeByKey(flowNode, nodeKey);
        processExecutor.moveToNextNode(flowInstance, currentNode);
    }

    private boolean existUncompletedTask(Long instanceId, String nodeKey) {
        List<FlowTask> flowTasks = flowTaskQuery.listTasksByInstanceIdAndNodeKey(instanceId, nodeKey);
        return flowTasks.stream()
                .filter(task -> !flowTaskAddSign.isAddSignGroupTask(task))
                // 已退回记录属于历史轮次，不参与当前会签的完成判断。
                .filter(task -> !Objects.equals(task.getStatus(), FlowTaskStatusEnum.RETURNED.getCode()))
                .anyMatch(task -> !Objects.equals(task.getStatus(), FlowTaskStatusEnum.APPROVE.getCode())
                        && !Objects.equals(task.getStatus(), FlowTaskStatusEnum.TRANSFERRED.getCode()));
    }
}
