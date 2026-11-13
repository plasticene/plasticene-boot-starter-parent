package com.plasticene.boot.flow.core.impl.task;

import cn.hutool.core.collection.CollUtil;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowDefinition;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowInstance;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTask;
import com.plasticene.boot.flow.core.impl.executor.FlowApproveTaskResult;
import com.plasticene.boot.flow.core.model.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.model.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.api.event.FlowEventType;
import com.plasticene.boot.flow.core.api.event.FlowTaskAction;
import com.plasticene.boot.flow.core.model.FlowExtraSetting;
import com.plasticene.boot.flow.core.model.FlowNode;
import com.plasticene.boot.flow.core.model.FlowTimeoutRule;
import com.plasticene.boot.flow.core.impl.tree.FlowReturnPath;
import com.plasticene.boot.flow.core.api.spi.FlowTaskAssigneeProvider;
import com.plasticene.boot.flow.core.impl.service.definition.FlowDefinitionService;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 创建流程节点任务并初始化审批与超时信息。
 * @author ZFJ
 * @since 2026-11-10
 */
@Service
public class FlowTaskCreation {
    @Resource
    private FlowTaskDAO flowTaskDAO;

    @Resource
    private FlowTaskAssigneeProvider flowTaskAssigneeProvider;

    @Resource
    private FlowDefinitionService flowDefinitionService;

    @Resource
    private FlowTaskQueries flowTaskQuery;

    @Resource
    private FlowTaskLifecycle flowTaskLifecycle;

    public FlowApproveTaskResult createApproveTask(FlowInstance instance, FlowNode currentNode) {
        Integer approveType = currentNode.getApproveType();
        // 自动通过
        if (Objects.equals(approveType, FlowNodeEnum.ApproveType.AUTO_PASS.getCode())) {
            createAutoApproveTask(instance, currentNode, FlowTaskStatusEnum.APPROVE, "系统自动通过");
            return FlowApproveTaskResult.APPROVED;
        }
        // 自动拒绝
        if (Objects.equals(approveType, FlowNodeEnum.ApproveType.AUTO_REJECT.getCode())) {
            createAutoApproveTask(instance, currentNode, FlowTaskStatusEnum.REJECT, "系统自动拒绝");
            return FlowApproveTaskResult.REJECTED;
        }
        // 人工手动审批
        List<Long> assignees = new ArrayList<>(flowTaskAssigneeProvider.getAssignees(currentNode, instance.getOrgId(), instance.getUserId()).stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        // 处理审批人为空
        Optional<FlowApproveTaskResult> result = processAssigneeEmpty(instance, currentNode, assignees);
        if (result.isPresent()) {
            return result.get();
        }
        // 处理发起人同审批人为同一人
        result = processSelfApprove(instance, currentNode, assignees);
        if (result.isPresent()) {
            return result.get();
        }
        // 未产生自动审批结果且仍无人处理时，保持停留，不额外兜底。
        if (CollUtil.isEmpty(assignees)) {
            return FlowApproveTaskResult.WAITING;
        }
        FlowDefinition definition = flowDefinitionService.getById(instance.getDefinitionId());
        FlowExtraSetting setting = FlowExtraSetting.normalize(
                definition == null ? null : definition.getExtraSetting());
        Set<Long> deduplicatedAssignees = findDeduplicatedAssignees(instance, currentNode, assignees, setting);
        deduplicatedAssignees.forEach(assignee -> createDeduplicatedApproveTask(instance, currentNode, assignee));
        assignees.removeAll(deduplicatedAssignees);
        Integer approveMode = currentNode.getApproveMode();
        if (Objects.equals(approveMode, FlowNodeEnum.ApproveMode.ANY.getCode())
                && CollUtil.isNotEmpty(deduplicatedAssignees)) {
            return FlowApproveTaskResult.APPROVED;
        }
        if (CollUtil.isEmpty(assignees)) {
            return FlowApproveTaskResult.APPROVED;
        }
        FlowTimeoutRule timeoutRule = FlowTimeoutRule.resolve(currentNode, setting);
        // 不是顺序审批 即会签或者或签
        if (!Objects.equals(approveMode, FlowNodeEnum.ApproveMode.ORDER.getCode())) {
            List<FlowTask> taskList = new ArrayList<>();
            assignees.forEach(assignee -> {
                FlowTask task = buildFlowTask(instance, currentNode);
                task.setAssignee(assignee);
                task.setStatus(FlowTaskStatusEnum.RUNNING.getCode());
                initializeTimeout(task, timeoutRule);
                taskList.add(task);
            });
            flowTaskDAO.insert(taskList);
            taskList.forEach(task -> flowTaskLifecycle.publishCreatedTask(instance, task));
            return FlowApproveTaskResult.WAITING;
        }
        // 顺序审批
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setStatus(FlowTaskStatusEnum.RUNNING.getCode());
        task.setAssignee(assignees.getFirst());
        task.setAssigneeList(assignees);
        initializeTimeout(task, timeoutRule);
        flowTaskDAO.insert(task);
        flowTaskLifecycle.publishCreatedTask(instance, task);
        return FlowApproveTaskResult.WAITING;
    }

    public void createCopyTask(FlowInstance instance, FlowNode currentNode) {
        List<Long> assignees = flowTaskAssigneeProvider.getAssignees(currentNode, instance.getOrgId(), instance.getUserId()).stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<FlowTask> taskList = new ArrayList<>();
        assignees.forEach(assignee -> {
            FlowTask task = buildFlowTask(instance, currentNode);
            task.setEndTime(LocalDateTime.now());
            task.setStatus(FlowTaskStatusEnum.APPROVE.getCode());
            task.setAssignee(assignee);
            taskList.add(task);
        });
        if (CollUtil.isNotEmpty(taskList)) {
            flowTaskDAO.insert(taskList);
        }
    }

    public void createStartTask(FlowInstance instance, FlowNode currentNode) {
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setEndTime(LocalDateTime.now());
        task.setStatus(FlowTaskStatusEnum.APPROVE.getCode());
        task.setAssignee(instance.getUserId());
        flowTaskDAO.insert(task);
    }

    public void createEndTask(FlowInstance instance, FlowNode currentNode) {
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setEndTime(LocalDateTime.now());
        task.setStatus(FlowTaskStatusEnum.APPROVE.getCode());
        flowTaskDAO.insert(task);
    }

    public void createBranchTask(FlowInstance instance, FlowNode currentNode) {
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setCompletedBranch(0);
        task.setEndTime(LocalDateTime.now());
        task.setStatus(FlowTaskStatusEnum.APPROVE.getCode());
        flowTaskDAO.insert(task);
    }

    public void createConditionNodeTask(FlowInstance instance, FlowNode conditionNode) {
        FlowTask task = new FlowTask();
        task.setOrgId(instance.getOrgId());
        task.setInstanceId(instance.getId());
        task.setNodeKey(conditionNode.getKey());
        task.setNodeName(conditionNode.getName());
        task.setNodeType(FlowNodeEnum.Type.CONDITION_NODE.getCode());
        task.setStartTime(LocalDateTime.now());
        task.setEndTime(LocalDateTime.now());
        task.setStatus(FlowTaskStatusEnum.APPROVE.getCode());
        flowTaskDAO.insert(task);
    }

    public void initializeTimeout(FlowTask task, FlowTimeoutRule rule) {
        task.setTimeoutRule(rule);
        task.setRemindCount(0);
        task.setTimeoutHandled(false);
        task.setDeadline(rule != null && Boolean.TRUE.equals(rule.getAutoEnabled())
                ? task.getStartTime().plusMinutes(FlowTimeoutRule.minutes(rule.getAutoAfter(), rule.getAutoUnit())) : null);
        task.setNextRemindTime(rule != null && Boolean.TRUE.equals(rule.getReminderEnabled())
                ? task.getStartTime().plusMinutes(FlowTimeoutRule.minutes(rule.getFirstReminder(), rule.getFirstReminderUnit())) : null);
    }

    private void createAutoApproveTask(FlowInstance instance,
                                        FlowNode currentNode,
                                        FlowTaskStatusEnum status,
                                        String comment) {
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setApproveType(Objects.equals(status, FlowTaskStatusEnum.APPROVE)
                ? FlowNodeEnum.ApproveType.AUTO_PASS.getCode()
                : FlowNodeEnum.ApproveType.AUTO_REJECT.getCode());
        task.setEndTime(LocalDateTime.now());
        task.setStatus(status.getCode());
        task.setComment(comment);
        flowTaskDAO.insert(task);
        flowTaskLifecycle.publishTask(instance, task, FlowEventType.TASK_COMPLETED, status.getCode(),
                status == FlowTaskStatusEnum.APPROVE ? FlowTaskAction.APPROVE : FlowTaskAction.REJECT, null);
    }

    private Optional<FlowApproveTaskResult> processAssigneeEmpty(FlowInstance instance, FlowNode currentNode, List<Long> assignees) {
        if (CollUtil.isNotEmpty(assignees)) {
            return Optional.empty();
        }
        // 处理审批人为空
        Integer assigneeEmpty = currentNode.getAssigneeEmpty();
        // 自动通过
        if (Objects.equals(assigneeEmpty, FlowNodeEnum.AssigneeEmpty.AUTO_APPROVE.getCode())) {
            createAutoApproveTask(instance, currentNode, FlowTaskStatusEnum.APPROVE, "审批人为空，系统自动通过");
            return Optional.of(FlowApproveTaskResult.APPROVED);
        }
        // 自动拒绝
        if (Objects.equals(assigneeEmpty, FlowNodeEnum.AssigneeEmpty.AUTO_REJECT.getCode())) {
            createAutoApproveTask(instance, currentNode, FlowTaskStatusEnum.REJECT, "审批人为空，系统自动拒绝");
            return Optional.of(FlowApproveTaskResult.REJECTED);
        }
        // 指定人员审批
        if (Objects.equals(assigneeEmpty, FlowNodeEnum.AssigneeEmpty.TO_USER.getCode())) {
            assignees.addAll(currentNode.getEmptyUserIds());
        }
        // 转给管理员审批
        if (Objects.equals(assigneeEmpty, FlowNodeEnum.AssigneeEmpty.TO_MANAGER.getCode())) {
            FlowDefinition definition = flowDefinitionService.getById(instance.getDefinitionId());
            assignees.addAll(definition.getManagerUserIds());
        }
        return Optional.empty();
    }

    private Optional<FlowApproveTaskResult> processSelfApprove(FlowInstance instance, FlowNode currentNode, List<Long> assignees) {
        Long userId = instance.getUserId();
        if (CollUtil.isEmpty(assignees) || !assignees.contains(userId)) {
            return Optional.empty();
        }
        Integer selfApprove = currentNode.getSelfApprove();
        // 自己对自己审批
        if (Objects.equals(selfApprove, FlowNodeEnum.SelfApprove.MANUAL.getCode())) {
            return Optional.empty();
        }
        // 自动跳过
        if (Objects.equals(selfApprove, FlowNodeEnum.SelfApprove.AUTO_SKIP.getCode())) {
            // 如果当前节点只有一个人，需要创建自动审批任务
            if (assignees.size() == 1) {
                FlowTask task = buildFlowTask(instance, currentNode);
                task.setEndTime(LocalDateTime.now());
                task.setStatus(FlowTaskStatusEnum.APPROVE.getCode());
                task.setApproveType(FlowNodeEnum.ApproveType.AUTO_PASS.getCode());
                task.setAssignee(assignees.getFirst());
                task.setComment("审批人与提交人是同一人，系统自动通过");
                flowTaskDAO.insert(task);
                flowTaskLifecycle.publishTask(instance, task, FlowEventType.TASK_COMPLETED, task.getStatus(), FlowTaskAction.APPROVE, null);
                return Optional.of(FlowApproveTaskResult.APPROVED);
            }
            // 如果当前节点有多个人，剔除自己
            assignees.remove(userId);
        }
        // 将本人替换成直属上级审批
        if (Objects.equals(selfApprove, FlowNodeEnum.SelfApprove.TO_LEADER.getCode())) {
            Long leaderId = flowTaskAssigneeProvider.getLeader(instance.getOrgId(), userId);
            assignees.set(assignees.indexOf(userId), leaderId);
        }
        // 将本人替换成部门 负责人审批
        if (Objects.equals(selfApprove, FlowNodeEnum.SelfApprove.TO_DEPT_LEADER.getCode())) {
            Long leaderId = flowTaskAssigneeProvider.getDeptLeader(instance.getOrgId(), userId);
            assignees.set(assignees.indexOf(userId), leaderId);
        }
        return Optional.empty();
    }

    private Set<Long> findDeduplicatedAssignees(FlowInstance instance, FlowNode currentNode,
                                                List<Long> assignees, FlowExtraSetting setting) {
        FlowExtraSetting.ApproverDeduplication strategy = setting.getApproverDeduplication();
        if (Objects.equals(strategy, FlowExtraSetting.ApproverDeduplication.NONE)) {
            return Set.of();
        }
        List<FlowTask> allTasks = flowTaskQuery.listTaskByInstanceId(instance.getId());
        // 重走节点必须重新审批，即使相同人员曾在其他节点审批过。
        if (allTasks.stream().anyMatch(task -> Objects.equals(task.getNodeKey(), currentNode.getKey())
                && !FlowReturnPath.isActive(task))) {
            return Set.of();
        }
        List<FlowTask> history = allTasks.stream()
                .filter(FlowReturnPath::isActive)
                .filter(task -> Objects.equals(task.getNodeType(), FlowNodeEnum.Type.APPROVE.getCode()))
                .filter(task -> Objects.equals(task.getStatus(), FlowTaskStatusEnum.APPROVE.getCode()))
                .filter(task -> task.getAssignee() != null)
                .toList();
        if (Objects.equals(strategy, FlowExtraSetting.ApproverDeduplication.ONCE_PER_PROCESS)) {
            Set<Long> manuallyApprovedUsers = history.stream()
                    .filter(task -> Objects.equals(task.getApproveType(), FlowNodeEnum.ApproveType.MANUAL.getCode()))
                    .filter(task -> !Boolean.TRUE.equals(task.getTimeoutHandled()))
                    .map(FlowTask::getAssignee)
                    .collect(Collectors.toSet());
            return assignees.stream().filter(manuallyApprovedUsers::contains).collect(Collectors.toSet());
        }
        FlowNode previousNode = currentNode.getParentNode();
        if (previousNode == null
                || !Objects.equals(previousNode.getType(), FlowNodeEnum.Type.APPROVE.getCode())) {
            return Set.of();
        }
        Set<Long> previousAssignees = history.stream()
                .filter(task -> Objects.equals(task.getNodeKey(), previousNode.getKey()))
                .map(FlowTask::getAssignee)
                .collect(Collectors.toSet());
        return assignees.stream().filter(previousAssignees::contains).collect(Collectors.toSet());
    }

    private void createDeduplicatedApproveTask(FlowInstance instance, FlowNode currentNode, Long assignee) {
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setAssignee(assignee);
        task.setApproveType(FlowNodeEnum.ApproveType.AUTO_PASS.getCode());
        task.setStatus(FlowTaskStatusEnum.APPROVE.getCode());
        task.setStartTime(LocalDateTime.now());
        task.setEndTime(LocalDateTime.now());
        task.setComment("审批人已审批，系统自动通过");
        flowTaskDAO.insert(task);
        flowTaskLifecycle.publishTask(instance, task, FlowEventType.TASK_COMPLETED, task.getStatus(), FlowTaskAction.APPROVE, null);
    }

    private FlowTask buildFlowTask(FlowInstance instance, FlowNode currentNode) {
        FlowTask task = new FlowTask();
        task.setOrgId(instance.getOrgId());
        task.setInstanceId(instance.getId());
        task.setNodeKey(currentNode.getKey());
        task.setNodeName(currentNode.getName());
        task.setNodeType(currentNode.getType());
        task.setApproveType(currentNode.getApproveType());
        task.setApproveMode(currentNode.getApproveMode());
        task.setRequireComment(currentNode.getRequireComment());
        task.setStartTime(LocalDateTime.now());
        return task;
    }
}
