package com.plasticene.boot.flow.core.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.utils.PtcBeanUtils;
import com.plasticene.boot.flow.core.dao.FlowFormChangeRecordDAO;
import com.plasticene.boot.flow.core.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.entity.FlowFormChangeRecord;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.entity.FlowTaskAddSign;
import com.plasticene.boot.flow.core.enums.FlowApproveTaskResult;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.event.FlowEventType;
import com.plasticene.boot.flow.core.event.FlowTaskAction;
import com.plasticene.boot.flow.core.event.FlowTaskCancelReason;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.param.FlowTaskAddSignParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskFormParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskReturnParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskTransferParam;
import com.plasticene.boot.flow.core.model.query.FlowTaskQuery;
import com.plasticene.boot.flow.core.model.vo.FlowTaskPageVO;
import com.plasticene.boot.flow.core.model.vo.FlowTaskReturnTargetVO;
import com.plasticene.boot.flow.core.parser.FlowParser;
import com.plasticene.boot.flow.core.provider.FlowOrganizationProvider;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import com.plasticene.boot.flow.core.service.task.FlowTaskAddSignOperations;
import com.plasticene.boot.flow.core.service.task.FlowTaskApproval;
import com.plasticene.boot.flow.core.service.task.FlowTaskCreation;
import com.plasticene.boot.flow.core.service.task.FlowTaskExecution;
import com.plasticene.boot.flow.core.service.task.FlowTaskLifecycle;
import com.plasticene.boot.flow.core.service.task.FlowTaskQueries;
import com.plasticene.boot.flow.core.service.task.FlowTaskReturn;
import com.plasticene.boot.flow.core.service.task.FlowTaskWithdraw;
import com.plasticene.boot.flow.core.utils.FlowFormChanges;
import com.plasticene.boot.flow.core.utils.FlowFormPermissions;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 流程任务服务入口，保留公开接口及事务边界。
 * @author ZFJ
 * @date 2025/9/2
 */
@Service
public class FlowTaskServiceImpl extends ServiceImpl<FlowTaskDAO, FlowTask> implements FlowTaskService {
    @Resource
    private FlowTaskDAO flowTaskDAO;

    @Resource
    @Lazy
    private FlowRuntimeService flowRuntimeService;

    @Resource
    private FlowDefinitionService flowDefinitionService;

    @Resource
    private FlowOrganizationProvider flowOrganizationProvider;

    @Resource
    private FlowFormChangeRecordDAO formChangeRecordDAO;

    @Resource
    private FlowTaskQueries flowTaskQuery;

    @Resource
    private FlowTaskCreation flowTaskCreation;

    @Resource
    private FlowTaskLifecycle flowTaskLifecycle;

    @Resource
    private FlowTaskExecution flowTaskExecution;

    @Resource
    private FlowTaskApproval flowTaskApproval;

    @Resource
    private FlowTaskAddSignOperations flowTaskAddSign;

    @Resource
    private FlowTaskReturn flowTaskReturn;

    @Resource
    private FlowTaskWithdraw flowTaskWithdraw;

    @Override
    public PageResult<FlowTaskPageVO> pageMyTodo(FlowTaskQuery query) {
        return flowTaskQuery.pageMyTodo(query);
    }

    @Override
    public PageResult<FlowTaskPageVO> pageMyDone(FlowTaskQuery query) {
        return flowTaskQuery.pageMyDone(query);
    }

    @Override
    public PageResult<FlowTaskPageVO> pageMyCopy(FlowTaskQuery query) {
        return flowTaskQuery.pageMyCopy(query);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public FlowApproveTaskResult createApproveTask(FlowInstance instance, FlowNode currentNode) {
        return flowTaskCreation.createApproveTask(instance, currentNode);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createCopyTask(FlowInstance instance, FlowNode currentNode) {
        flowTaskCreation.createCopyTask(instance, currentNode);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createStartTask(FlowInstance instance, FlowNode currentNode) {
        flowTaskCreation.createStartTask(instance, currentNode);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createEndTask(FlowInstance instance, FlowNode currentNode) {
        flowTaskCreation.createEndTask(instance, currentNode);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createBranchTask(FlowInstance instance, FlowNode currentNode) {
        flowTaskCreation.createBranchTask(instance, currentNode);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createConditionNodeTask(FlowInstance instance, FlowNode conditionNode) {
        flowTaskCreation.createConditionNodeTask(instance, conditionNode);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void saveForm(FlowTaskFormParam param) {
        LoginUser user = flowTaskExecution.getLoginUser();
        flowTaskExecution.execute(param.getTaskId(), user, (task, instance) -> {
            FlowDefinition definition = flowDefinitionService.getById(instance.getDefinitionId());
            if (definition == null || !Objects.equals(definition.getOrgId(), user.getOrgId())) {
                throw new BizException("流程发布定义不存在");
            }
            var permissions = FlowFormPermissions.resolve(
                    definition, instance, List.of(task), user.getId(), task.getId());
            var variables = FlowFormPermissions.mergeChanges(
                    instance.getVarMap(), param.getFormData(), permissions);
            var changes = FlowFormChanges.diff(definition.getFormConfig(), instance.getVarMap(), variables);
            if (changes.isEmpty()) {
                return;
            }
            FlowInstance update = new FlowInstance();
            update.setId(instance.getId());
            update.setVarMap(variables);
            if (!flowRuntimeService.updateById(update)) {
                throw new BizException("表单保存失败，请刷新后重试");
            }
            FlowFormChangeRecord record = new FlowFormChangeRecord();
            record.setOrgId(instance.getOrgId());
            record.setInstanceId(instance.getId());
            record.setTaskId(task.getId());
            record.setNodeKey(task.getNodeKey());
            record.setNodeName(FlowParser.findNodeByKey(definition.getModelNode(), task.getNodeKey()).getName());
            record.setOperatorId(user.getId());
            record.setOperatorName(flowOrganizationProvider.getUserMap().getOrDefault(user.getId(), String.valueOf(user.getId())));
            record.setOperationTime(LocalDateTime.now());
            record.setFieldDetails(changes);
            if (formChangeRecordDAO.insert(record) != 1) {
                throw new BizException("表单变更记录保存失败，请刷新后重试");
            }
        });
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void approveTask(FlowTaskParam param) {
        approveTask(param, flowTaskExecution.getLoginUser());
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void approveTask(FlowTaskParam param, LoginUser loginUser) {
        Long taskId = param.getTaskId();
        String comment = param.getComment();
        FlowTask initialTask = flowTaskExecution.getExecutableTask(taskId, loginUser);
        if (Objects.equals(initialTask.getRequireComment(), CommonConstant.IS_ON) && StrUtil.isBlank(comment)) {
            throw new BizException("意见不能为空");
        }
        flowTaskExecution.execute(initialTask, loginUser, (task, instance) ->
                flowTaskApproval.approve(task, instance, loginUser, comment));
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void rejectTask(FlowTaskParam param) {
        rejectTask(param, flowTaskExecution.getLoginUser());
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void rejectTask(FlowTaskParam param, LoginUser loginUser) {
        Long taskId = param.getTaskId();
        String comment = param.getComment();
        FlowTask initialTask = flowTaskExecution.getExecutableTask(taskId, loginUser);
        if (Objects.equals(initialTask.getRequireComment(), CommonConstant.IS_ON) && StrUtil.isBlank(comment)) {
            throw new BizException("意见不能为空");
        }
        String nodeKey = initialTask.getNodeKey();
        flowTaskExecution.execute(initialTask, loginUser, (task, instance) ->
                flowTaskApproval.reject(taskId, nodeKey, instance, loginUser, comment));
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void addSign(FlowTaskAddSignParam param) {
        LoginUser user = flowTaskExecution.getLoginUser();
        flowTaskExecution.execute(param.getTaskId(), user, (task, instance) ->
                flowTaskAddSign.addSign(param, task, instance, user));
    }

    @Override
    public boolean isAddSignGroupTask(FlowTask task) {
        return flowTaskAddSign.isAddSignGroupTask(task);
    }

    @Override
    public List<FlowTaskAddSign> listAddSigns(Long instanceId) {
        return flowTaskQuery.listAddSigns(instanceId);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void transferTask(FlowTaskTransferParam param) {
        LoginUser loginUser = flowTaskExecution.getLoginUser();
        flowTaskExecution.execute(param.getTaskId(), loginUser, (task, instance) -> {
            flowTaskAddSign.requireOriginalNodeTask(task);
            flowTaskExecution.requireNodeOperation(flowTaskExecution.getReturnModel(instance), task, FlowNode::getAllowTransfer, "转交");
            Long assignee = param.getAssignee();
            if (Objects.equals(assignee, loginUser.getId())) {
                throw new BizException("不能转交给自己");
            }
            if (!flowOrganizationProvider.getActiveUserMap().containsKey(assignee)) {
                throw new BizException("接收人不存在、已禁用或不属于当前组织");
            }
            List<FlowTask> nodeTasks = flowTaskQuery.listTasksByInstanceIdAndNodeKey(task.getInstanceId(), task.getNodeKey());
            if (nodeTasks.stream().anyMatch(item -> Objects.equals(item.getAssignee(), assignee))
                    || task.getAssigneeList() != null && task.getAssigneeList().contains(assignee)) {
                throw new BizException("不能转交给当前节点已有的审批人");
            }
            List<Long> assignees = task.getAssigneeList() == null ? null : new ArrayList<>(task.getAssigneeList());
            if (Objects.equals(task.getApproveMode(), FlowNodeEnum.ApproveMode.ORDER.getCode())) {
                int index = assignees == null ? -1 : assignees.indexOf(task.getAssignee());
                if (index < 0) {
                    throw new BizException("顺序审批人员数据异常，不能转交");
                }
                assignees.set(index, assignee);
            }
            LocalDateTime now = LocalDateTime.now();
            FlowTask history = new FlowTask();
            history.setStatus(FlowTaskStatusEnum.TRANSFERRED.getCode());
            history.setEndTime(now);
            history.setComment(param.getComment().trim());
            history.setTransferTo(assignee);
            int updated = flowTaskDAO.update(history, new LambdaUpdateWrapper<FlowTask>()
                    .eq(FlowTask::getId, task.getId())
                    .isNull(FlowTask::getReturnTaskId)
                    .eq(FlowTask::getOrgId, loginUser.getOrgId())
                    .eq(FlowTask::getAssignee, loginUser.getId())
                    .eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode())
                    .eq(FlowTask::getIsDelete, CommonConstant.IS_NOT_DEL));
            if (updated != 1) {
                throw new BizException("任务已发生变化，请刷新后重试");
            }
            // 复制待办快照，保留截止时间、提醒轮次与下一次提醒时间，不重新计时。
            FlowTask next = PtcBeanUtils.copy(task, FlowTask.class);
            next.setId(null);
            next.setSourceTaskId(task.getId());
            next.setTransferTo(null);
            next.setAssignee(assignee);
            next.setAssigneeList(assignees);
            next.setStartTime(now);
            next.setEndTime(null);
            next.setComment(null);
            next.setTimeoutHandled(false);
            next.setCreator(null);
            next.setUpdater(null);
            next.setCreateTime(null);
            next.setUpdateTime(null);
            flowTaskLifecycle.publishTask(instance, task, FlowEventType.TASK_COMPLETED, FlowTaskStatusEnum.TRANSFERRED.getCode(),
                    FlowTaskAction.TRANSFER, null);
            flowTaskDAO.insert(next);
            flowTaskLifecycle.publishCreatedTask(instance, next);
        });
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public List<FlowTaskReturnTargetVO> listReturnTargets(Long taskId) {
        LoginUser user = flowTaskExecution.getLoginUser();
        return flowTaskExecution.withTask(taskId, user, (task, instance) ->
                flowTaskReturn.listReturnTargets(task, instance));
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void returnTask(FlowTaskReturnParam param) {
        String comment = param.getComment() == null ? "" : param.getComment().trim();
        if (comment.isEmpty() || comment.length() > 500) {
            throw new BizException("退回原因须为1至500字");
        }
        LoginUser user = flowTaskExecution.getLoginUser();
        flowTaskExecution.execute(param.getTaskId(), user, (task, instance) ->
                flowTaskReturn.returnTask(param, task, instance, user, comment));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void withdrawTask(Long taskId) {
        flowTaskWithdraw.withdrawTask(taskId);
    }

    @Override
    public boolean canWithdrawTask(FlowTask task, FlowInstance instance,
                                   FlowDefinition definition, List<FlowTask> instanceTasks) {
        return flowTaskWithdraw.canWithdrawTask(task, instance, definition, instanceTasks);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addCompletedBranches(Long instanceId, String nodeKey, int count) {
        if (count <= 0) {
            return;
        }
        flowTaskDAO.addCompletedBranches(instanceId, nodeKey, count);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int incrementCompletedBranchAndGet(Long instanceId, String nodeKey) {
        flowTaskDAO.incrementCompletedBranch(instanceId, nodeKey);
        LambdaQueryWrapper<FlowTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FlowTask::getInstanceId, instanceId);
        queryWrapper.eq(FlowTask::getNodeKey, nodeKey);
        queryWrapper.isNull(FlowTask::getReturnTaskId);
        queryWrapper.eq(FlowTask::getIsDelete, CommonConstant.IS_NOT_DEL);
        FlowTask flowTask = flowTaskDAO.selectOne(queryWrapper);
        return flowTask.getCompletedBranch();
    }

    @Override
    public void delInstanceRunningTask(Long instanceId) {
        flowTaskAddSign.cancelRunningAddSigns(instanceId, null, FlowTaskCancelReason.INSTANCE_REJECTED);
        flowTaskLifecycle.delInstanceRunningTask(instanceId);
    }

    @Override
    public void cancelInstanceRunningTask(Long instanceId) {
        flowTaskAddSign.cancelRunningAddSigns(instanceId, null, FlowTaskCancelReason.INSTANCE_CANCELLED);
        flowTaskLifecycle.cancelInstanceRunningTask(instanceId);
    }

    @Override
    public List<FlowTask> listTaskByInstanceId(Long instanceId) {
        return flowTaskQuery.listTaskByInstanceId(instanceId);
    }
}
