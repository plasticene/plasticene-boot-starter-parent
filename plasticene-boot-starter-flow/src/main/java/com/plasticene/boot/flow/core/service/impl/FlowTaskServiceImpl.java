package com.plasticene.boot.flow.core.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.plasticene.boot.flow.core.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.dto.ProcessNode;
import com.plasticene.boot.flow.core.dto.ProcessNodeCondition;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.enums.FlowProcessNodeEnum;
import com.plasticene.boot.flow.core.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.provider.FlowTaskAssigneeProvider;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createApproveTask(FlowInstance instance, ProcessNode currentNode) {
        Integer approveType = currentNode.getApproveType();
        if (Objects.equals(approveType, FlowProcessNodeEnum.ApproveType.AUTO_PASS.getCode())) {
            FlowTask task = buildFlowTask(instance, currentNode);
            task.setEndTime(new Date());
            task.setStatus(FlowTaskStatusEnum.COMPLETE.getCode());
            task.setComment("系统自动审批通过");
            flowTaskDAO.insert(task);
        }
        if (Objects.equals(approveType, FlowProcessNodeEnum.ApproveType.AUTO_REJECT.getCode())) {
            FlowTask task = buildFlowTask(instance, currentNode);
            task.setEndTime(new Date());
            task.setStatus(FlowTaskStatusEnum.REJECT.getCode());
            task.setComment("系统自动拒绝");
            flowTaskDAO.insert(task);
        }
        if (Objects.equals(approveType, FlowProcessNodeEnum.ApproveType.MANUAL.getCode())) {
            List<Long> assignees = flowTaskAssigneeProvider.getAssignees(currentNode);
            List<FlowTask> taskList = new ArrayList<>();
            assignees.forEach(assignee -> {
                FlowTask task = buildFlowTask(instance, currentNode);
                task.setAssignee(assignee);
                task.setStatus(FlowTaskStatusEnum.RUNNING.getCode());
                taskList.add(task);
            });

            if (CollUtil.isNotEmpty(taskList)) {
                flowTaskDAO.insert(taskList);
            }
            // todo 审批模式为顺序审批时，需适配
        }


    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createCopyTask(FlowInstance instance, ProcessNode currentNode) {
        List<Long> assignees = flowTaskAssigneeProvider.getAssignees(currentNode);
        List<FlowTask> taskList = new ArrayList<>();
        assignees.forEach(assignee -> {
            FlowTask task = buildFlowTask(instance, currentNode);
            task.setEndTime(new Date());
            task.setStatus(FlowTaskStatusEnum.COMPLETE.getCode());
            task.setAssignee(instance.getUserId());
            taskList.add(task);
        });
        if (CollUtil.isNotEmpty(taskList)) {
            flowTaskDAO.insert(taskList);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createEndTask(FlowInstance instance, ProcessNode currentNode) {
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setEndTime(new Date());
        task.setStatus(FlowTaskStatusEnum.COMPLETE.getCode());
        flowTaskDAO.insert(task);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createConditionBranchTask(FlowInstance instance, ProcessNode currentNode) {
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setEndTime(new Date());
        task.setStatus(FlowTaskStatusEnum.COMPLETE.getCode());
        flowTaskDAO.insert(task);
    }

    @Override
    public void createConditionNodeTask(FlowInstance instance, ProcessNodeCondition conditionNode) {
        FlowTask task = new FlowTask();
        task.setOrgId(instance.getOrgId());
        task.setInstanceId(instance.getId());
        task.setNodeKey(conditionNode.getKey());
        task.setNodeName(conditionNode.getName());
        task.setNodeType(FlowProcessNodeEnum.Type.CONDITION_NODE.getCode());
        task.setStartTime(new Date());
        task.setEndTime(new Date());
        task.setStatus(FlowTaskStatusEnum.COMPLETE.getCode());
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        flowTaskDAO.insert(task);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createStartTask(FlowInstance instance, ProcessNode currentNode) {
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setEndTime(new Date());
        task.setStatus(FlowTaskStatusEnum.COMPLETE.getCode());
        task.setAssignee(instance.getUserId());
        flowTaskDAO.insert(task);
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
        task.setStartTime(new Date());
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        return task;
    }

}
