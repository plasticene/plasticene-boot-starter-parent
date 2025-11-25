package com.plasticene.boot.flow.core.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.model.dto.ProcessNode;
import com.plasticene.boot.flow.core.model.dto.ProcessNodeCondition;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowProcess;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowProcessNodeEnum;
import com.plasticene.boot.flow.core.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.executor.ProcessInstanceExecutor;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;
import com.plasticene.boot.flow.core.parser.FlowParser;
import com.plasticene.boot.flow.core.provider.FlowTaskAssigneeProvider;
import com.plasticene.boot.flow.core.service.FlowProcessService;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import com.plasticene.boot.mybatis.core.query.PtcLambdaQueryWrapper;
import jakarta.annotation.Resource;
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
public class FlowTaskServiceImpl implements FlowTaskService {
    @Resource
    private FlowTaskDAO flowTaskDAO;
    @Resource
    private FlowTaskAssigneeProvider flowTaskAssigneeProvider;
    @Resource
    private FlowRuntimeService flowRuntimeService;
    @Resource
    private ProcessInstanceExecutor processInstanceExecutor;
    @Resource
    private FlowProcessService flowProcessService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createApproveTask(FlowInstance instance, ProcessNode currentNode) {
        Integer approveType = currentNode.getApproveType();
        // 自动通过
        if (Objects.equals(approveType, FlowProcessNodeEnum.ApproveType.AUTO_PASS.getCode())) {
            FlowTask task = buildFlowTask(instance, currentNode);
            task.setEndTime(LocalDateTime.now());
            task.setStatus(FlowTaskStatusEnum.COMPLETE.getCode());
            task.setComment("系统自动通过");
            flowTaskDAO.insert(task);
            return task.getId();
        }
        // 自动拒绝
        if (Objects.equals(approveType, FlowProcessNodeEnum.ApproveType.AUTO_REJECT.getCode())) {
            FlowTask task = buildFlowTask(instance, currentNode);
            task.setEndTime(LocalDateTime.now());
            task.setStatus(FlowTaskStatusEnum.REJECT.getCode());
            task.setComment("系统自动拒绝");
            flowTaskDAO.insert(task);
            return task.getId();
        }
        // 人工手动审批
        List<Long> assignees = flowTaskAssigneeProvider.getAssignees(currentNode);
        Integer approveMode = currentNode.getApproveMode();
        // 不是顺序审批 即会签或者或签
        if (!Objects.equals(approveMode, FlowProcessNodeEnum.ApproveMode.ORDER.getCode())) {
            List<FlowTask> taskList = new ArrayList<>();
            assignees.forEach(assignee -> {
                FlowTask task = buildFlowTask(instance, currentNode);
                task.setAssignee(assignee);
                task.setStatus(FlowTaskStatusEnum.RUNNING.getCode());
                taskList.add(task);
            });
            flowTaskDAO.insert(taskList);
            return null;
        }
        // 顺序审批
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setStatus(FlowTaskStatusEnum.RUNNING.getCode());
        task.setAssignee(assignees.getFirst());
        task.setAssigneeList(assignees);
        flowTaskDAO.insert(task);
        return task.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createCopyTask(FlowInstance instance, ProcessNode currentNode) {
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
    public void createStartTask(FlowInstance instance, ProcessNode currentNode) {
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setEndTime(LocalDateTime.now());
        task.setStatus(FlowTaskStatusEnum.COMPLETE.getCode());
        task.setAssignee(instance.getUserId());
        flowTaskDAO.insert(task);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createEndTask(FlowInstance instance, ProcessNode currentNode) {
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setEndTime(LocalDateTime.now());
        task.setStatus(FlowTaskStatusEnum.COMPLETE.getCode());
        flowTaskDAO.insert(task);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createBranchTask(FlowInstance instance, ProcessNode currentNode) {
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setEndTime(LocalDateTime.now());
        task.setStatus(FlowTaskStatusEnum.COMPLETE.getCode());
        flowTaskDAO.insert(task);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createConditionNodeTask(FlowInstance instance, ProcessNodeCondition conditionNode) {
        FlowTask task = new FlowTask();
        task.setOrgId(instance.getOrgId());
        task.setInstanceId(instance.getId());
        task.setNodeKey(conditionNode.getKey());
        task.setNodeName(conditionNode.getName());
        task.setNodeType(FlowProcessNodeEnum.Type.CONDITION_NODE.getCode());
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
        if (Objects.equals(approveMode, FlowProcessNodeEnum.ApproveMode.ALL.getCode())) {
            boolean existed = existUncompletedTask(instanceId, nodeKey);
            // 会签存在其他人未审批
            if (existed) {
                return;
            }
        }
        // 或签
        if (Objects.equals(approveMode, FlowProcessNodeEnum.ApproveMode.ANY.getCode())) {
            // 再次查询任务
            flowTask = flowTaskDAO.selectById(taskId);
            if (Objects.equals(flowTask.getIsDelete(), CommonConstant.IS_DEL)) {
                throw new BizException("当前节点已审批，不能再审");
            }
            deleteOtherRunningTask(instanceId, taskId);
        }
        // 顺序审批
        if (Objects.equals(approveMode, FlowProcessNodeEnum.ApproveMode.ORDER.getCode())) {
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
        ProcessNode processNode = flowProcess.getProcessNode();
        FlowParser.makeParentNode(processNode);
        ProcessNode currentNode = FlowParser.findNodeByKey(processNode, nodeKey);
        processInstanceExecutor.moveToNextNode(flowInstance, currentNode);


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
        deleteOtherRunningTask(instanceId, taskId);

        // 终止流程实例
        FlowProcess flowProcess = flowProcessService.getById(instance.getProcessId());
        ProcessNode processNode = flowProcess.getProcessNode();
        FlowParser.makeParentNode(processNode);
        ProcessNode currentNode = FlowParser.findNodeByKey(processNode, nodeKey);
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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteOtherRunningTask(Long instanceId, Long taskId) {
        LambdaUpdateWrapper<FlowTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FlowTask::getInstanceId, instanceId);
        updateWrapper.eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode());
        updateWrapper.ne(FlowTask::getId, taskId);
        updateWrapper.set(FlowTask::getIsDelete, CommonConstant.IS_DEL);
        flowTaskDAO.update(updateWrapper);
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


    private FlowTask buildFlowTask(FlowInstance instance, ProcessNode currentNode) {
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
