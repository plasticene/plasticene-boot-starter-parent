package com.plasticene.boot.flow.core.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowProcess;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;
import com.plasticene.boot.flow.core.parser.FlowParser;
import com.plasticene.boot.flow.core.provider.FlowTaskAssigneeProvider;
import com.plasticene.boot.flow.core.service.FlowProcessService;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import com.plasticene.boot.mybatis.core.query.PtcLambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
@Service
public class FlowTaskServiceImpl extends ServiceImpl<FlowTaskDAO, FlowTask> implements FlowTaskService {
    @Resource
    private FlowTaskDAO flowTaskDAO;
    @Resource
    private FlowTaskAssigneeProvider flowTaskAssigneeProvider;
    @Resource
    private FlowRuntimeService flowRuntimeService;
    @Resource
    @Lazy
    private ProcessExecutor processExecutor;
    @Resource
    private FlowProcessService flowProcessService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createApproveTask(FlowInstance instance, FlowNode currentNode) {
        Integer approveType = currentNode.getApproveType();
        // 自动通过
        if (Objects.equals(approveType, FlowNodeEnum.ApproveType.AUTO_PASS.getCode())) {
            createAutoApproveTask(instance, currentNode, FlowTaskStatusEnum.COMPLETE, "系统自动通过");
            return;
        }
        // 自动拒绝
        if (Objects.equals(approveType, FlowNodeEnum.ApproveType.AUTO_REJECT.getCode())) {
            createAutoApproveTask(instance, currentNode, FlowTaskStatusEnum.REJECT, "系统自动拒绝");
            return;
        }
        // 人工手动审批
        List<Long> assignees = flowTaskAssigneeProvider.getAssignees(currentNode);
        // 处理审批人为空
        processAssigneeEmpty(instance, currentNode, assignees);
        // 处理发起人同审批人为同一人
        processSelfApprove(instance, currentNode, assignees);
        // 经过上面审批人为空、审批人和发起人同一人逻辑处理之后审批人还是为空，肯定已经执行自动逻辑了，return即可
        if (CollUtil.isEmpty(assignees)) {
            return;
        }
        Integer approveMode = currentNode.getApproveMode();
        // 不是顺序审批 即会签或者或签
        if (!Objects.equals(approveMode, FlowNodeEnum.ApproveMode.ORDER.getCode())) {
            List<FlowTask> taskList = new ArrayList<>();
            assignees.forEach(assignee -> {
                FlowTask task = buildFlowTask(instance, currentNode);
                task.setAssignee(assignee);
                task.setStatus(FlowTaskStatusEnum.RUNNING.getCode());
                taskList.add(task);
            });
            flowTaskDAO.insert(taskList);
            return;
        }
        // 顺序审批
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setStatus(FlowTaskStatusEnum.RUNNING.getCode());
        task.setAssignee(assignees.getFirst());
        task.setAssigneeList(assignees);
        flowTaskDAO.insert(task);
    }

    private void createAutoApproveTask(FlowInstance instance,
                                        FlowNode currentNode,
                                        FlowTaskStatusEnum status,
                                        String comment) {
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setEndTime(LocalDateTime.now());
        task.setStatus(status.getCode());
        task.setComment(comment);
        flowTaskDAO.insert(task);
    }

    private void processAssigneeEmpty(FlowInstance instance, FlowNode currentNode, List<Long> assignees) {
        if (CollUtil.isNotEmpty(assignees)) {
            return;
        }
        // 处理审批人为空
        Integer assigneeEmpty = currentNode.getAssigneeEmpty();
        // 自动通过
        if (Objects.equals(assigneeEmpty, FlowNodeEnum.AssigneeEmpty.AUTO_APPROVE.getCode())) {
            createAutoApproveTask(instance, currentNode, FlowTaskStatusEnum.COMPLETE, "审批人为空，系统自动通过");
            // 移动到下一个节点
            processExecutor.moveToNextNode(instance, currentNode);
        }
        // 自动拒绝
        if (Objects.equals(assigneeEmpty, FlowNodeEnum.AssigneeEmpty.AUTO_REJECT.getCode())) {
            createAutoApproveTask(instance, currentNode, FlowTaskStatusEnum.REJECT, "审批人为空，系统自动拒绝");
            delInstanceRunningTask(instance.getId());
            flowRuntimeService.endInstance(instance, currentNode, FlowInstanceStatusEnum.REJECT);
        }
        // 指定人员审批
        if (Objects.equals(assigneeEmpty, FlowNodeEnum.AssigneeEmpty.TO_USER.getCode())) {
            assignees.addAll(currentNode.getEmptyUserIds());
        }
        // 转给管理员审批
        if (Objects.equals(assigneeEmpty, FlowNodeEnum.AssigneeEmpty.TO_MANAGER.getCode())) {
            FlowProcess process = flowProcessService.getById(instance.getProcessId());
            assignees.addAll(process.getManagerUserIds());
        }
    }

    private void processSelfApprove(FlowInstance instance, FlowNode currentNode, List<Long> assignees) {
        Long userId = instance.getUserId();
        if (CollUtil.isEmpty(assignees) || !assignees.contains(userId)) {
            return;
        }
        Integer selfApprove = currentNode.getSelfApprove();
        // 自己对自己审批
        if (Objects.equals(selfApprove, FlowNodeEnum.SelfApprove.MANUAL.getCode())) {
            return;
        }
        // 自动跳过
        if (Objects.equals(selfApprove, FlowNodeEnum.SelfApprove.AUTO_SKIP.getCode())) {
            // 如果当前节点只有一个人，需要创建自动审批任务
            if (assignees.size() == 1) {
                FlowTask task = buildFlowTask(instance, currentNode);
                task.setEndTime(LocalDateTime.now());
                task.setStatus(FlowTaskStatusEnum.COMPLETE.getCode());
                task.setAssignee(assignees.getFirst());
                task.setComment("审批人与提交人是同一人，系统自动通过");
                flowTaskDAO.insert(task);
                // 移动到下一个节点
                processExecutor.moveToNextNode(instance, currentNode);
            }
            // 如果当前节点有多个人，剔除自己
            assignees.remove(userId);
        }
        // 将本人替换成直属上级审批
        if (Objects.equals(selfApprove, FlowNodeEnum.SelfApprove.TO_LEADER.getCode())) {
            Long leaderId = flowTaskAssigneeProvider.getLeader(userId);
            assignees.set(assignees.indexOf(userId), leaderId);
        }
        // 将本人替换成部门 负责人审批
        if (Objects.equals(selfApprove, FlowNodeEnum.SelfApprove.TO_DEPT_LEADER.getCode())) {
            Long leaderId = flowTaskAssigneeProvider.getDeptLeader(userId);
            assignees.set(assignees.indexOf(userId), leaderId);
        }
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createCopyTask(FlowInstance instance, FlowNode currentNode) {
        List<Long> assignees = flowTaskAssigneeProvider.getAssignees(currentNode);
        List<FlowTask> taskList = new ArrayList<>();
        assignees.forEach(assignee -> {
            FlowTask task = buildFlowTask(instance, currentNode);
            task.setEndTime(LocalDateTime.now());
            task.setStatus(FlowTaskStatusEnum.COMPLETE.getCode());
            task.setAssignee(assignee);
            taskList.add(task);
        });
        if (CollUtil.isNotEmpty(taskList)) {
            flowTaskDAO.insert(taskList);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createStartTask(FlowInstance instance, FlowNode currentNode) {
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setEndTime(LocalDateTime.now());
        task.setStatus(FlowTaskStatusEnum.COMPLETE.getCode());
        task.setAssignee(instance.getUserId());
        flowTaskDAO.insert(task);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createEndTask(FlowInstance instance, FlowNode currentNode) {
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setEndTime(LocalDateTime.now());
        task.setStatus(FlowTaskStatusEnum.COMPLETE.getCode());
        flowTaskDAO.insert(task);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createBranchTask(FlowInstance instance, FlowNode currentNode) {
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setEndTime(LocalDateTime.now());
        task.setStatus(FlowTaskStatusEnum.COMPLETE.getCode());
        flowTaskDAO.insert(task);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createConditionNodeTask(FlowInstance instance, FlowNode conditionNode) {
        FlowTask task = new FlowTask();
        task.setOrgId(instance.getOrgId());
        task.setInstanceId(instance.getId());
        task.setNodeKey(conditionNode.getKey());
        task.setNodeName(conditionNode.getName());
        task.setNodeType(FlowNodeEnum.Type.CONDITION_NODE.getCode());
        task.setStartTime(LocalDateTime.now());
        task.setEndTime(LocalDateTime.now());
        task.setStatus(FlowTaskStatusEnum.COMPLETE.getCode());
        flowTaskDAO.insert(task);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void approveTask(FlowTaskParam param) {
        Long taskId = param.getTaskId();
        String comment = param.getComment();
        FlowTask flowTask = flowTaskDAO.selectById(taskId);
        Integer requireComment = flowTask.getRequireComment();
        if (Objects.equals(requireComment, CommonConstant.IS_ON) && StrUtil.isBlank(comment)) {
            throw new BizException("意见不能为空");
        }
        Long instanceId = flowTask.getInstanceId();
        String nodeKey = flowTask.getNodeKey();
        Integer approveMode = flowTask.getApproveMode();
        FlowInstance flowInstance = flowRuntimeService.selectInstanceForUpdate(instanceId);
        // 完成任务
        completeTask(taskId, comment, FlowTaskStatusEnum.COMPLETE);
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
            // 再次查询任务
            flowTask = flowTaskDAO.selectById(taskId);
            if (Objects.equals(flowTask.getIsDelete(), CommonConstant.IS_DEL)) {
                throw new BizException("当前节点已审批，不能再审");
            }
            delInstanceRunningTask(instanceId);
        }
        // 顺序审批
        if (Objects.equals(approveMode, FlowNodeEnum.ApproveMode.ORDER.getCode())) {
            Long assignee = flowTask.getAssignee();
            List<Long> assigneeList = flowTask.getAssigneeList();
            int index = assigneeList.indexOf(assignee);
            if (index < assigneeList.size() - 1) {
                // 不是当前节点最后一个审批人，插入下一个审批人任务，不流入下个节点
                flowTask.setId(null);
                flowTask.setAssignee(assigneeList.get(index + 1));
                flowTask.setStatus(FlowTaskStatusEnum.RUNNING.getCode());
                flowTask.setStartTime(LocalDateTime.now());
                flowTask.setEndTime(null);
                flowTask.setCreator(null);
                flowTask.setUpdater(null);
                flowTask.setCreateTime(null);
                flowTask.setUpdateTime(null);
                flowTaskDAO.insert(flowTask);
                return;
            }
        }

        // 根据流程模型查找当前节点并流转下一个节点
        FlowProcess flowProcess = flowProcessService.getById(flowInstance.getProcessId());
        FlowNode flowNode = flowProcess.getFlowNode();
        FlowParser.makeParentNode(flowNode);
        FlowNode currentNode = FlowParser.findNodeByKey(flowNode, nodeKey);
        processExecutor.moveToNextNode(flowInstance, currentNode);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void rejectTask(FlowTaskParam param) {
        Long taskId = param.getTaskId();
        String comment = param.getComment();
        FlowTask flowTask = flowTaskDAO.selectById(taskId);
        Integer requireComment = flowTask.getRequireComment();
        if (Objects.equals(requireComment, CommonConstant.IS_ON) && StrUtil.isBlank(comment)) {
            throw new BizException("意见不能为空");
        }
        Long instanceId = flowTask.getInstanceId();
        String nodeKey = flowTask.getNodeKey();
        FlowInstance instance = flowRuntimeService.selectInstanceForUpdate(instanceId);
        // 再次查询任务
        flowTask = flowTaskDAO.selectById(taskId);
        if (Objects.equals(flowTask.getIsDelete(), CommonConstant.IS_DEL)) {
            throw new BizException("当前节点已审批，不能再审");
        }
        completeTask(taskId, comment, FlowTaskStatusEnum.REJECT);
        delInstanceRunningTask(instanceId);

        // 终止流程实例
        FlowProcess flowProcess = flowProcessService.getById(instance.getProcessId());
        FlowNode flowNode = flowProcess.getFlowNode();
        FlowParser.makeParentNode(flowNode);
        FlowNode currentNode = FlowParser.findNodeByKey(flowNode, nodeKey);
        flowRuntimeService.endInstance(instance, currentNode, FlowInstanceStatusEnum.REJECT);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int incrementCompletedBranchAndGet(Long instanceId, String nodeKey) {
        flowTaskDAO.incrementCompletedBranch(instanceId, nodeKey);
        LambdaQueryWrapper<FlowTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FlowTask::getInstanceId, instanceId);
        queryWrapper.eq(FlowTask::getNodeKey, nodeKey);
        FlowTask flowTask = flowTaskDAO.selectOne(queryWrapper);
        return flowTask.getCompletedBranch();
    }

    @Override
    public void delInstanceRunningTask(Long instanceId) {
        LambdaUpdateWrapper<FlowTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FlowTask::getInstanceId, instanceId);
        updateWrapper.eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode());
        updateWrapper.set(FlowTask::getIsDelete, CommonConstant.IS_DEL);
        flowTaskDAO.update(updateWrapper);
    }

    @Override
    public List<FlowTask> listTaskByInstanceId(Long instanceId) {
        PtcLambdaQueryWrapper<FlowTask> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowTask::getInstanceId, instanceId).eq(FlowTask::getIsDelete, CommonConstant.IS_NOT_DEL);
        queryWrapper.orderByAsc(FlowTask::getId);
        return flowTaskDAO.selectList(queryWrapper);
    }

    private void completeTask(Long taskId, String comment, FlowTaskStatusEnum statusEnum) {
        FlowTask task = new FlowTask();
        task.setId(taskId);
        task.setStatus(statusEnum.getCode());
        task.setComment(comment);
        task.setEndTime(LocalDateTime.now());
        LambdaUpdateWrapper<FlowTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FlowTask::getId, taskId).eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode());
        int update = flowTaskDAO.update(task, updateWrapper);
        if (update != 1) {
            throw new BizException("任务已经处理过，请勿重复处理");
        }
    }

    private boolean existUncompletedTask(Long instanceId, String nodeKey) {
        List<FlowTask> flowTasks = listTasksByInstanceIdAndNodeKey(instanceId, nodeKey);
        return flowTasks.stream()
                .anyMatch(task -> !Objects.equals(task.getStatus(), FlowTaskStatusEnum.COMPLETE.getCode()));
    }

    private List<FlowTask> listTasksByInstanceIdAndNodeKey(Long instanceId, String nodeKey) {
        PtcLambdaQueryWrapper<FlowTask> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowTask::getInstanceId, instanceId);
        queryWrapper.eq(FlowTask::getNodeKey, nodeKey);
        return flowTaskDAO.selectList(queryWrapper);
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
        task.setStartTime(LocalDateTime.now());
        return task;
    }

}
