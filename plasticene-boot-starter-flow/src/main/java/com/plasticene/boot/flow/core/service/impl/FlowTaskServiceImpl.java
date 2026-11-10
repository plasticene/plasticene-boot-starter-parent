package com.plasticene.boot.flow.core.service.impl;

import com.plasticene.boot.flow.core.enums.FlowApproveTaskResult;
import com.plasticene.boot.flow.core.utils.FlowFormPermissions;
import com.plasticene.boot.flow.core.utils.FlowFormChanges;
import com.plasticene.boot.flow.core.dao.FlowFormChangeRecordDAO;
import com.plasticene.boot.flow.core.entity.FlowFormChangeRecord;
import com.plasticene.boot.flow.core.model.param.FlowTaskFormParam;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.common.utils.PtcBeanUtils;
import com.plasticene.boot.flow.core.dao.FlowTaskAddSignDAO;
import com.plasticene.boot.flow.core.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.dao.FlowTimeoutNoticeDAO;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.entity.FlowTaskAddSign;
import com.plasticene.boot.flow.core.entity.FlowTimeoutNotice;
import com.plasticene.boot.flow.core.enums.FlowAddSignEnum;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowTimeoutNoticeEnums;
import com.plasticene.boot.flow.core.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.event.FlowEventPublisher;
import com.plasticene.boot.flow.core.event.FlowEventType;
import com.plasticene.boot.flow.core.event.FlowTaskEvent;
import com.plasticene.boot.flow.core.event.FlowTaskAction;
import com.plasticene.boot.flow.core.event.FlowTaskCancelReason;
import com.plasticene.boot.flow.core.model.dto.FlowExtraSetting;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.dto.FlowTimeoutRule;
import com.plasticene.boot.flow.core.model.param.FlowTaskAddSignParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskReturnParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskTransferParam;
import com.plasticene.boot.flow.core.model.query.FlowTaskQuery;
import com.plasticene.boot.flow.core.model.vo.FlowTaskPageVO;
import com.plasticene.boot.flow.core.model.vo.FlowTaskReturnTargetVO;
import com.plasticene.boot.flow.core.parser.FlowParser;
import com.plasticene.boot.flow.core.parser.FlowReturnPath;
import com.plasticene.boot.flow.core.provider.FlowOrganizationProvider;
import com.plasticene.boot.flow.core.provider.FlowTaskAssigneeProvider;
import com.plasticene.boot.flow.core.service.FlowCategoryService;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import com.plasticene.boot.mybatis.core.query.PtcLambdaQueryWrapper;
import com.plasticene.boot.mybatis.core.utils.MybatisUtils;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
@Service
public class FlowTaskServiceImpl extends ServiceImpl<FlowTaskDAO, FlowTask> implements FlowTaskService {
    private static final List<Integer> DONE_STATUSES = List.of(
            FlowTaskStatusEnum.APPROVE.getCode(),
            FlowTaskStatusEnum.REJECT.getCode(),
            FlowTaskStatusEnum.CANCEL.getCode(),
            FlowTaskStatusEnum.TRANSFERRED.getCode(),
            FlowTaskStatusEnum.RETURNED.getCode()
    );
    private static final List<Integer> INSTANCE_STATUSES = Arrays.stream(FlowInstanceStatusEnum.values())
            .map(FlowInstanceStatusEnum::getCode)
            .toList();

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
    private FlowDefinitionService flowDefinitionService;
    @Resource
    private FlowCategoryService flowCategoryService;
    @Resource
    private FlowOrganizationProvider flowOrganizationProvider;
    @Resource
    private FlowTimeoutNoticeDAO flowTimeoutNoticeDAO;

    @Resource
    private FlowTaskAddSignDAO flowTaskAddSignDAO;

    @Resource
    private FlowFormChangeRecordDAO formChangeRecordDAO;

    @Resource
    private FlowEventPublisher flowEventPublisher;

    @Override
    public PageResult<FlowTaskPageVO> pageMyTodo(FlowTaskQuery query) {
        query.setNodeType(FlowNodeEnum.Type.APPROVE.getCode());
        query.setStatuses(List.of(FlowTaskStatusEnum.RUNNING.getCode(), FlowTaskStatusEnum.WAITING_ADD_SIGN.getCode()));
        return pageMyTask(query);
    }

    @Override
    public PageResult<FlowTaskPageVO> pageMyDone(FlowTaskQuery query) {
        query.setNodeType(FlowNodeEnum.Type.APPROVE.getCode());
        List<Integer> statuses = query.getStatuses();
        if (CollUtil.isEmpty(statuses)) {
            query.setStatuses(DONE_STATUSES);
        } else {
            boolean invalidStatus = statuses.stream().anyMatch(status -> !DONE_STATUSES.contains(status));
            if (invalidStatus) {
                throw new BizException("已办任务状态仅支持已完成、拒绝、已取消、已转交或已退回");
            }
            query.setStatuses(statuses.stream().distinct().toList());
        }
        return pageMyTask(query);
    }

    @Override
    public PageResult<FlowTaskPageVO> pageMyCopy(FlowTaskQuery query) {
        query.setNodeType(FlowNodeEnum.Type.COPY.getCode());
        query.setStatuses(List.of(FlowTaskStatusEnum.APPROVE.getCode()));
        return pageMyTask(query);
    }

    private PageResult<FlowTaskPageVO> pageMyTask(FlowTaskQuery query) {
        query.setKeyword(StrUtil.trim(query.getKeyword()));
        query.setCategory(StrUtil.trim(query.getCategory()));
        normalizeInstanceStatuses(query);
        validateTimeRange(query.getTaskStartTimeBegin(), query.getTaskStartTimeEnd(), "任务接收时间");
        validateTimeRange(query.getTaskEndTimeBegin(), query.getTaskEndTimeEnd(), "任务完成时间");

        LoginUser loginUser = getLoginUser();
        query.setAssignee(loginUser.getId());
        query.setOrgId(loginUser.getOrgId());
        IPage<FlowTaskPageVO> page = flowTaskDAO.pageTask(MybatisUtils.buildPage(query), query);

        Map<String, String> categoryMap = flowCategoryService.getCategoryMap();
        Map<Long, String> userMap = flowOrganizationProvider.getUserMap();
        LocalDateTime now = LocalDateTime.now();
        page.getRecords().forEach(vo -> enrichTaskPageVO(vo, categoryMap, userMap, now));
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getPages());
    }

    private void validateTimeRange(LocalDateTime begin, LocalDateTime end, String fieldName) {
        if (begin != null && end != null && begin.isAfter(end)) {
            throw new BizException(fieldName + "开始值不能晚于结束值");
        }
    }

    private void enrichTaskPageVO(FlowTaskPageVO vo,
                                  Map<String, String> categoryMap,
                                  Map<Long, String> userMap,
                                  LocalDateTime now) {
        String category = vo.getCategory();
        vo.setCategoryName(category == null ? null : categoryMap.getOrDefault(category, category));
        Long startUserId = vo.getStartUserId();
        vo.setStartUserName(startUserId == null
                ? null
                : userMap.getOrDefault(startUserId, String.valueOf(startUserId)));
        FlowInstanceStatusEnum instanceStatus = getInstanceStatus(vo.getInstanceStatus());
        vo.setInstanceStatusName(instanceStatus == null ? null : instanceStatus.getName());
        if (vo.getTaskStartTime() == null) {
            return;
        }
        LocalDateTime endTime = vo.getTaskEndTime() == null ? now : vo.getTaskEndTime();
        vo.setElapsedTime(Math.max(0, Duration.between(vo.getTaskStartTime(), endTime).getSeconds()));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
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
        List<Long> assignees = new ArrayList<>(flowTaskAssigneeProvider.getAssignees(currentNode).stream()
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
            taskList.forEach(task -> publishCreatedTask(instance, task));
            return FlowApproveTaskResult.WAITING;
        }
        // 顺序审批
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setStatus(FlowTaskStatusEnum.RUNNING.getCode());
        task.setAssignee(assignees.getFirst());
        task.setAssigneeList(assignees);
        initializeTimeout(task, timeoutRule);
        flowTaskDAO.insert(task);
        publishCreatedTask(instance, task);
        return FlowApproveTaskResult.WAITING;
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
        publishTask(instance, task, FlowEventType.TASK_COMPLETED, status.getCode(),
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
                publishTask(instance, task, FlowEventType.TASK_COMPLETED, task.getStatus(), FlowTaskAction.APPROVE, null);
                return Optional.of(FlowApproveTaskResult.APPROVED);
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
        return Optional.empty();
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createCopyTask(FlowInstance instance, FlowNode currentNode) {
        List<Long> assignees = flowTaskAssigneeProvider.getAssignees(currentNode).stream()
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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createStartTask(FlowInstance instance, FlowNode currentNode) {
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setEndTime(LocalDateTime.now());
        task.setStatus(FlowTaskStatusEnum.APPROVE.getCode());
        task.setAssignee(instance.getUserId());
        flowTaskDAO.insert(task);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createEndTask(FlowInstance instance, FlowNode currentNode) {
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setEndTime(LocalDateTime.now());
        task.setStatus(FlowTaskStatusEnum.APPROVE.getCode());
        flowTaskDAO.insert(task);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createBranchTask(FlowInstance instance, FlowNode currentNode) {
        FlowTask task = buildFlowTask(instance, currentNode);
        task.setCompletedBranch(0);
        task.setEndTime(LocalDateTime.now());
        task.setStatus(FlowTaskStatusEnum.APPROVE.getCode());
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
        task.setStatus(FlowTaskStatusEnum.APPROVE.getCode());
        flowTaskDAO.insert(task);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void saveForm(FlowTaskFormParam param) {
        LoginUser user = getLoginUser();
        FlowTask task = getExecutableTask(param.getTaskId(), user);
        FlowInstance instance = flowRuntimeService.selectInstanceForUpdate(task.getInstanceId());
        task = getExecutableTask(param.getTaskId(), user);
        validateRunningInstance(instance, user, task);
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
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void approveTask(FlowTaskParam param) {
        approveTask(param, getLoginUser());
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void approveTask(FlowTaskParam param, LoginUser loginUser) {
        Long taskId = param.getTaskId();
        String comment = param.getComment();
        FlowTask flowTask = getExecutableTask(taskId, loginUser);
        Integer requireComment = flowTask.getRequireComment();
        if (Objects.equals(requireComment, CommonConstant.IS_ON) && StrUtil.isBlank(comment)) {
            throw new BizException("意见不能为空");
        }
        Long instanceId = flowTask.getInstanceId();
        FlowInstance flowInstance = flowRuntimeService.selectInstanceForUpdate(instanceId);
        flowTask = getExecutableTask(taskId, loginUser);
        validateRunningInstance(flowInstance, loginUser, flowTask);
        if (isAddSignGroupTask(flowTask)) {
            approveAddSignTask(flowTask, loginUser, comment, flowInstance);
            return;
        }
        completeTask(flowTask, flowInstance, loginUser, comment, FlowTaskStatusEnum.APPROVE);
        settleNodeApproval(flowTask, flowInstance);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void rejectTask(FlowTaskParam param) {
        rejectTask(param, getLoginUser());
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void rejectTask(FlowTaskParam param, LoginUser loginUser) {
        Long taskId = param.getTaskId();
        String comment = param.getComment();
        FlowTask flowTask = getExecutableTask(taskId, loginUser);
        Integer requireComment = flowTask.getRequireComment();
        if (Objects.equals(requireComment, CommonConstant.IS_ON) && StrUtil.isBlank(comment)) {
            throw new BizException("意见不能为空");
        }
        Long instanceId = flowTask.getInstanceId();
        String nodeKey = flowTask.getNodeKey();
        FlowInstance instance = flowRuntimeService.selectInstanceForUpdate(instanceId);
        flowTask = getExecutableTask(taskId, loginUser);
        validateRunningInstance(instance, loginUser, flowTask);
        // 再次查询任务
        flowTask = flowTaskDAO.selectById(taskId);
        if (Objects.equals(flowTask.getIsDelete(), CommonConstant.IS_DEL)) {
            throw new BizException("当前节点已审批，不能再审");
        }
        completeTask(flowTask, instance, loginUser, comment, FlowTaskStatusEnum.REJECT);
        if (isAddSignGroupTask(flowTask)) {
            finishAddSign(getAddSignBatch(flowTask), FlowAddSignEnum.State.REJECTED);
        }
        delInstanceRunningTask(instanceId);

        // 终止流程实例
        FlowDefinition flowDefinition = flowDefinitionService.getById(instance.getDefinitionId());
        FlowNode flowNode = flowDefinition.getModelNode();
        FlowParser.makeParentNode(flowNode);
        FlowNode currentNode = FlowParser.findNodeByKey(flowNode, nodeKey);
        flowRuntimeService.endInstance(instance, currentNode, FlowInstanceStatusEnum.REJECT);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void addSign(FlowTaskAddSignParam param) {
        LoginUser user = getLoginUser();
        FlowTask task = getExecutableTask(param.getTaskId(), user);
        FlowInstance instance = flowRuntimeService.selectInstanceForUpdate(task.getInstanceId());
        task = getExecutableTask(param.getTaskId(), user);
        validateRunningInstance(instance, user, task);
        requireOriginalNodeTask(task);
        requireNodeOperation(getReturnModel(instance), task, FlowNode::getAllowAddSign, "加签");
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
        publishTask(instance, task, FlowEventType.TASK_CANCELLED, FlowTaskStatusEnum.WAITING_ADD_SIGN.getCode(),
                null, FlowTaskCancelReason.ADD_SIGN_WAITING);
        cancelPendingReminders(List.of(task.getId()));
        List<Long> starters = Objects.equals(batch.getApproveMode(), FlowNodeEnum.ApproveMode.ORDER.getCode())
                ? List.of(batch.getAssigneeIds().getFirst()) : batch.getAssigneeIds();
        for (Long id : starters) {
            createAddSignTask(task, batch, id);
        }
    }

    @Override
    public boolean isAddSignGroupTask(FlowTask task) {
        FlowTaskAddSign batch = getAddSignBatch(task);
        return batch != null && batch.getSignType() != FlowAddSignEnum.Type.PARALLEL;
    }

    @Override
    public List<FlowTaskAddSign> listAddSigns(Long instanceId) {
        return flowTaskAddSignDAO.selectList(new LambdaQueryWrapper<FlowTaskAddSign>()
                .eq(FlowTaskAddSign::getInstanceId, instanceId).orderByAsc(FlowTaskAddSign::getId));
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void transferTask(FlowTaskTransferParam param) {
        LoginUser loginUser = getLoginUser();
        FlowTask task = getExecutableTask(param.getTaskId(), loginUser);
        FlowInstance instance = flowRuntimeService.selectInstanceForUpdate(task.getInstanceId());
        // 与人工审批、取消和超时处理使用同一实例锁，锁内重读最新任务。
        task = getExecutableTask(param.getTaskId(), loginUser);
        validateRunningInstance(instance, loginUser, task);
        requireOriginalNodeTask(task);
        requireNodeOperation(getReturnModel(instance), task, FlowNode::getAllowTransfer, "转交");
        Long assignee = param.getAssignee();
        if (Objects.equals(assignee, loginUser.getId())) {
            throw new BizException("不能转交给自己");
        }
        if (!flowOrganizationProvider.getActiveUserMap().containsKey(assignee)) {
            throw new BizException("接收人不存在、已禁用或不属于当前组织");
        }
        List<FlowTask> nodeTasks = listTasksByInstanceIdAndNodeKey(task.getInstanceId(), task.getNodeKey());
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
        publishTask(instance, task, FlowEventType.TASK_COMPLETED, FlowTaskStatusEnum.TRANSFERRED.getCode(),
                FlowTaskAction.TRANSFER, null);
        flowTaskDAO.insert(next);
        publishCreatedTask(instance, next);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public List<FlowTaskReturnTargetVO> listReturnTargets(Long taskId) {
        LoginUser user = getLoginUser();
        // 先判断任务状态是否可以执行
        FlowTask task = getExecutableTask(taskId, user);
        // 获取实例锁
        FlowInstance instance = flowRuntimeService.selectInstanceForUpdate(task.getInstanceId());
        // 再次判断任务状态是否可以执行
        task = getExecutableTask(taskId, user);
        validateRunningInstance(instance, user, task);
        requireOriginalNodeTask(task);
        FlowNode root = getReturnModel(instance);
        requireNodeOperation(root, task, FlowNode::getAllowReturn, "退回");
        // 获取可退回的目标节点
        return FlowReturnPath.targets(root, task.getNodeKey(), listTaskByInstanceId(instance.getId())).stream()
                .map(node -> new FlowTaskReturnTargetVO(node.getKey(), node.getName())).toList();
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Override
    public void returnTask(FlowTaskReturnParam param) {
        String comment = param.getComment() == null ? "" : param.getComment().trim();
        if (comment.isEmpty() || comment.length() > 500) {
            throw new BizException("退回原因须为1至500字");
        }
        LoginUser user = getLoginUser();
        FlowTask task = getExecutableTask(param.getTaskId(), user);
        FlowInstance instance = flowRuntimeService.selectInstanceForUpdate(task.getInstanceId());
        task = getExecutableTask(param.getTaskId(), user);
        validateRunningInstance(instance, user, task);
        requireOriginalNodeTask(task);
        FlowNode root = getReturnModel(instance);
        requireNodeOperation(root, task, FlowNode::getAllowReturn, "退回");
        List<FlowTask> history = listTaskByInstanceId(instance.getId());
        FlowNode target = FlowReturnPath.targets(root, task.getNodeKey(), history).stream()
                .filter(node -> Objects.equals(node.getKey(), param.getTargetNodeKey())).findFirst()
                .orElseThrow(() -> new BizException("退回目标不是有效的已完成前序人工审批节点，请刷新后重试"));
        FlowNode current = FlowParser.findNodeByKey(root, task.getNodeKey());
        Set<String> affectedKeys = FlowReturnPath.reachable(target);
        List<FlowTask> affected = history.stream().filter(FlowReturnPath::isActive)
                .filter(item -> affectedKeys.contains(item.getNodeKey())).toList();
        Long returnTaskId = task.getId();
        LocalDateTime now = LocalDateTime.now();
        completeTask(task, instance, user, comment, FlowTaskStatusEnum.RETURNED);
        flowTaskDAO.update(new LambdaUpdateWrapper<FlowTask>()
                .eq(FlowTask::getId, returnTaskId).set(FlowTask::getReturnTargetNodeKey, target.getKey()));
        cancelRunningAddSigns(instance.getId(), affectedKeys, FlowTaskCancelReason.RETURN);
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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void withdrawTask(Long taskId) {
        if (taskId == null) {
            throw new BizException("任务id不能为空");
        }
        LoginUser loginUser = getLoginUser();
        FlowTask task = flowTaskDAO.selectById(taskId);
        if (task == null
                || !Objects.equals(task.getOrgId(), loginUser.getOrgId())
                || !Objects.equals(task.getAssignee(), loginUser.getId())) {
            throw new BizException("任务不存在或无权撤回");
        }
        FlowInstance instance = flowRuntimeService.selectInstanceForUpdate(task.getInstanceId());
        FlowDefinition definition = flowDefinitionService.getById(instance.getDefinitionId());
        List<FlowTask> instanceTasks = listTaskByInstanceId(instance.getId());
        if (!canWithdrawTask(task, instance, definition, instanceTasks)) {
            throw new BizException("当前审批结果不允许撤回");
        }

        boolean sameNode = Objects.equals(instance.getCurrentNodeKey(), task.getNodeKey());
        if (!sameNode || Objects.equals(task.getApproveMode(), FlowNodeEnum.ApproveMode.ORDER.getCode())) {
            List<FlowTask> cancelled = runningApprovalTasks(instance.getId());
            LambdaUpdateWrapper<FlowTask> cancelRunningWrapper = new LambdaUpdateWrapper<>();
            cancelRunningWrapper.isNull(FlowTask::getReturnTaskId);
            cancelRunningWrapper.eq(FlowTask::getInstanceId, instance.getId())
                    .eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode())
                    .eq(FlowTask::getIsDelete, CommonConstant.IS_NOT_DEL)
                    .set(FlowTask::getIsDelete, CommonConstant.IS_DEL);
            flowTaskDAO.update(cancelRunningWrapper);
            publishCancelledTasks(cancelled, FlowTaskCancelReason.WITHDRAW);
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

        publishCreatedTask(instance, task);
        FlowNode node = new FlowNode();
        node.setKey(task.getNodeKey());
        node.setName(task.getNodeName());
        flowRuntimeService.updateInstanceCurrentNode(instance.getId(), node);
    }

    @Override
    public boolean canWithdrawTask(FlowTask task, FlowInstance instance,
                                   FlowDefinition definition, List<FlowTask> instanceTasks) {
        LoginUser loginUser = LoginUserHolder.get();
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
        List<FlowTaskAddSign> addSigns = listAddSigns(task.getInstanceId());
        if (isAddSignGroupTask(task) || addSigns.stream()
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
        cancelRunningAddSigns(instanceId, null, FlowTaskCancelReason.INSTANCE_REJECTED);
        List<FlowTask> cancelled = runningApprovalTasks(instanceId);
        LambdaUpdateWrapper<FlowTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FlowTask::getInstanceId, instanceId);
        updateWrapper.eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode());
        updateWrapper.isNull(FlowTask::getReturnTaskId);
        updateWrapper.set(FlowTask::getIsDelete, CommonConstant.IS_DEL);
        flowTaskDAO.update(updateWrapper);
        publishCancelledTasks(cancelled, FlowTaskCancelReason.INSTANCE_REJECTED);
    }

    @Override
    public void cancelInstanceRunningTask(Long instanceId) {
        cancelRunningAddSigns(instanceId, null, FlowTaskCancelReason.INSTANCE_CANCELLED);
        List<FlowTask> cancelled = runningApprovalTasks(instanceId);
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

    @Override
    public List<FlowTask> listTaskByInstanceId(Long instanceId) {
        PtcLambdaQueryWrapper<FlowTask> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowTask::getInstanceId, instanceId)
                .eq(FlowTask::getIsDelete, CommonConstant.IS_NOT_DEL);
        queryWrapper.orderByAsc(FlowTask::getId);
        return flowTaskDAO.selectList(queryWrapper);
    }


    private void publishCreatedTask(FlowInstance instance, FlowTask task) {
        publishTask(instance, task, FlowEventType.TASK_CREATED, FlowTaskStatusEnum.RUNNING.getCode(), null, null);
    }

    private void publishTask(FlowInstance instance, FlowTask task, FlowEventType type, Integer status,
                             FlowTaskAction action, FlowTaskCancelReason reason) {
        flowEventPublisher.publish(FlowTaskEvent.of(instance, task, type, status, action, reason));
    }

    private List<FlowTask> runningApprovalTasks(Long instanceId) {
        return flowTaskDAO.selectList(new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getInstanceId, instanceId)
                .eq(FlowTask::getNodeType, FlowNodeEnum.Type.APPROVE.getCode())
                .eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode())
                .eq(FlowTask::getIsDelete, CommonConstant.IS_NOT_DEL)
                .isNull(FlowTask::getReturnTaskId));
    }

    private void publishCancelledTasks(List<FlowTask> tasks, FlowTaskCancelReason reason) {
        if (tasks.isEmpty()) {
            return;
        }
        FlowInstance instance = flowRuntimeService.getById(tasks.getFirst().getInstanceId());
        for (FlowTask task : tasks) {
            publishTask(instance, task, FlowEventType.TASK_CANCELLED,
                    FlowTaskStatusEnum.CANCEL.getCode(), null, reason);
        }
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
            cancelRunningAddSigns(instanceId, Set.of(nodeKey), FlowTaskCancelReason.NODE_COMPLETED);
            // 再次查询任务
            flowTask = flowTaskDAO.selectById(taskId);
            if (Objects.equals(flowTask.getIsDelete(), CommonConstant.IS_DEL)) {
                throw new BizException("当前节点已审批，不能再审");
            }
            List<FlowTask> cancelled = runningApprovalTasks(instanceId).stream()
                    .filter(item -> Objects.equals(item.getNodeKey(), nodeKey)).toList();
            // 或签只结束当前审批节点，不能删除其他并行分支的待办。
            LambdaUpdateWrapper<FlowTask> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(FlowTask::getInstanceId, instanceId);
            updateWrapper.eq(FlowTask::getNodeKey, nodeKey);
            updateWrapper.eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode());
            updateWrapper.isNull(FlowTask::getReturnTaskId);
            updateWrapper.set(FlowTask::getIsDelete, CommonConstant.IS_DEL);
            flowTaskDAO.update(updateWrapper);
            publishCancelledTasks(cancelled, FlowTaskCancelReason.NODE_COMPLETED);
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
                flowTask.setAddSignId(findParallelAddSignId(flowTask, flowTask.getAssignee()));
                flowTask.setStatus(FlowTaskStatusEnum.RUNNING.getCode());
                flowTask.setStartTime(LocalDateTime.now());
                flowTask.setEndTime(null);
                flowTask.setCreator(null);
                flowTask.setUpdater(null);
                flowTask.setCreateTime(null);
                flowTask.setUpdateTime(null);
                flowTask.setComment(null);
                initializeTimeout(flowTask, flowTask.getTimeoutRule());
                flowTaskDAO.insert(flowTask);
                publishCreatedTask(flowInstance, flowTask);
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
        Map<Long, String> activeUsers = flowOrganizationProvider.getActiveUserMap();
        if (ids.contains(user.getId()) || ids.stream().anyMatch(id -> !activeUsers.containsKey(id))) {
            throw new BizException("加签人员不能为本人，且须为本组织启用用户");
        }
        List<FlowTask> tasks = listTasksByInstanceIdAndNodeKey(task.getInstanceId(), task.getNodeKey());
        Set<Long> existing = tasks.stream().map(FlowTask::getAssignee).filter(Objects::nonNull).collect(Collectors.toSet());
        tasks.stream().map(FlowTask::getAssigneeList).filter(Objects::nonNull).forEach(existing::addAll);
        if (ids.stream().anyMatch(existing::contains)) {
            throw new BizException("不能选择当前节点已有或排队中的审批人");
        }
        if (Objects.equals(task.getApproveMode(), FlowNodeEnum.ApproveMode.ORDER.getCode())
                && (task.getAssigneeList() == null || !task.getAssigneeList().contains(task.getAssignee()))) {
            throw new BizException("顺序审批人员数据异常，不能加签");
        }
        if (listAddSigns(task.getInstanceId()).stream().anyMatch(batch -> Objects.equals(batch.getSourceTaskId(), task.getId())
                && batch.getState() == FlowAddSignEnum.State.RUNNING)) {
            throw new BizException("当前任务已在等待加签");
        }
    }

    private FlowTaskAddSign getAddSignBatch(FlowTask task) {
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

    private void requireOriginalNodeTask(FlowTask task) {
        if (isAddSignGroupTask(task)) {
            throw new BizException("前后加签任务仅支持通过或拒绝");
        }
    }

    private void createAddSignTask(FlowTask source, FlowTaskAddSign batch, Long assignee) {
        FlowTask task = PtcBeanUtils.copy(source, FlowTask.class);
        resetTaskForAssignee(task, assignee);
        task.setAddSignId(batch.getId());
        task.setApproveMode(batch.getApproveMode());
        task.setAssigneeList(batch.getSignType() == FlowAddSignEnum.Type.PARALLEL ? null : batch.getAssigneeIds());
        initializeTimeout(task, source.getTimeoutRule());
        flowTaskDAO.insert(task);
        publishCreatedTask(flowRuntimeService.getById(task.getInstanceId()), task);
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

    private void approveAddSignTask(FlowTask task, LoginUser user, String comment, FlowInstance instance) {
        FlowTaskAddSign batch = getAddSignBatch(task);
        FlowTask source = flowTaskDAO.selectById(batch.getSourceTaskId());
        if (batch.getState() != FlowAddSignEnum.State.RUNNING || source == null
                || !Objects.equals(source.getStatus(), FlowTaskStatusEnum.WAITING_ADD_SIGN.getCode())
                || !FlowReturnPath.isActive(source) || !Objects.equals(source.getIsDelete(), CommonConstant.IS_NOT_DEL)) {
            throw new BizException("加签已结束，请刷新后查看");
        }
        completeTask(task, instance, user, comment, FlowTaskStatusEnum.APPROVE);
        if (Objects.equals(batch.getApproveMode(), FlowNodeEnum.ApproveMode.ALL.getCode())) {
            List<FlowTask> members = flowTaskDAO.selectList(new LambdaQueryWrapper<FlowTask>()
                    .eq(FlowTask::getAddSignId, batch.getId()).eq(FlowTask::getIsDelete, CommonConstant.IS_NOT_DEL));
            if (members.stream().anyMatch(member -> !Objects.equals(member.getStatus(), FlowTaskStatusEnum.APPROVE.getCode()))) {
                return;
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
                return;
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
            publishCreatedTask(instance, source);
        }
        if (batch.getSignType() == FlowAddSignEnum.Type.AFTER) {
            LoginUser owner = new LoginUser();
            owner.setId(source.getAssignee());
            owner.setOrgId(source.getOrgId());
            completeTask(source, instance, owner, batch.getComment(), FlowTaskStatusEnum.APPROVE);
            settleNodeApproval(source, instance);
        }
    }

    private void finishAddSign(FlowTaskAddSign batch, FlowAddSignEnum.State state) {
        batch.setState(state);
        batch.setEndTime(LocalDateTime.now());
        flowTaskAddSignDAO.updateById(batch);
    }

    private Long findParallelAddSignId(FlowTask task, Long assignee) {
        Set<Long> sources = listTasksByInstanceIdAndNodeKey(task.getInstanceId(), task.getNodeKey()).stream()
                .map(FlowTask::getId).collect(Collectors.toSet());
        return listAddSigns(task.getInstanceId()).stream()
                .filter(batch -> batch.getSignType() == FlowAddSignEnum.Type.PARALLEL
                        && batch.getState() == FlowAddSignEnum.State.APPLIED
                        && Objects.equals(batch.getNodeKey(), task.getNodeKey())
                        && sources.contains(batch.getSourceTaskId()) && batch.getAssigneeIds().contains(assignee))
                .map(FlowTaskAddSign::getId).findFirst().orElse(null);
    }

    private void cancelRunningAddSigns(Long instanceId, Set<String> nodeKeys, FlowTaskCancelReason reason) {
        for (FlowTaskAddSign batch : listAddSigns(instanceId)) {
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
            cancelPendingReminders(List.of(batch.getSourceTaskId()));
        }
    }

    private void cancelAddSignMembers(FlowTaskAddSign batch, FlowTaskCancelReason reason) {
        List<FlowTask> members = flowTaskDAO.selectList(new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getAddSignId, batch.getId()).eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode()));
        flowTaskDAO.update(new LambdaUpdateWrapper<FlowTask>().eq(FlowTask::getAddSignId, batch.getId())
                .eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode())
                .set(FlowTask::getStatus, FlowTaskStatusEnum.CANCEL.getCode())
                .set(FlowTask::getEndTime, LocalDateTime.now()).set(FlowTask::getNextRemindTime, null));
        publishCancelledTasks(members, reason);
        cancelPendingReminders(members.stream().map(FlowTask::getId).toList());
    }

    private void cancelPendingReminders(List<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return;
        }
        flowTimeoutNoticeDAO.update(new LambdaUpdateWrapper<FlowTimeoutNotice>().in(FlowTimeoutNotice::getTaskId, taskIds)
                .eq(FlowTimeoutNotice::getEventType, FlowTimeoutNoticeEnums.EventType.REMINDER.getCode())
                .eq(FlowTimeoutNotice::getState, FlowTimeoutNoticeEnums.State.PENDING.getCode())
                .set(FlowTimeoutNotice::getState, FlowTimeoutNoticeEnums.State.CANCELLED.getCode()));
    }

    private void completeTask(FlowTask source, FlowInstance instance,
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

    private boolean existUncompletedTask(Long instanceId, String nodeKey) {
        List<FlowTask> flowTasks = listTasksByInstanceIdAndNodeKey(instanceId, nodeKey);
        return flowTasks.stream()
                .filter(task -> !isAddSignGroupTask(task))
                // 已退回记录属于历史轮次，不参与当前会签的完成判断。
                .filter(task -> !Objects.equals(task.getStatus(), FlowTaskStatusEnum.RETURNED.getCode()))
                .anyMatch(task -> !Objects.equals(task.getStatus(), FlowTaskStatusEnum.APPROVE.getCode())
                        && !Objects.equals(task.getStatus(), FlowTaskStatusEnum.TRANSFERRED.getCode()));
    }

    private List<FlowTask> listTasksByInstanceIdAndNodeKey(Long instanceId, String nodeKey) {
        PtcLambdaQueryWrapper<FlowTask> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowTask::getInstanceId, instanceId);
        queryWrapper.eq(FlowTask::getNodeKey, nodeKey);
        queryWrapper.isNull(FlowTask::getReturnTaskId);
        queryWrapper.eq(FlowTask::getIsDelete, CommonConstant.IS_NOT_DEL);
        return flowTaskDAO.selectList(queryWrapper);
    }

    private Set<Long> findDeduplicatedAssignees(FlowInstance instance, FlowNode currentNode,
                                                List<Long> assignees, FlowExtraSetting setting) {
        FlowExtraSetting.ApproverDeduplication strategy = setting.getApproverDeduplication();
        if (Objects.equals(strategy, FlowExtraSetting.ApproverDeduplication.NONE)) {
            return Set.of();
        }
        List<FlowTask> allTasks = listTaskByInstanceId(instance.getId());
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
        publishTask(instance, task, FlowEventType.TASK_COMPLETED, task.getStatus(), FlowTaskAction.APPROVE, null);
    }

    private LoginUser getLoginUser() {
        LoginUser loginUser = LoginUserHolder.get();
        if (loginUser == null || loginUser.getId() == null || loginUser.getOrgId() == null) {
            throw new BizException("登录用户信息不存在");
        }
        return loginUser;
    }

    private void normalizeInstanceStatuses(FlowTaskQuery query) {
        List<Integer> statuses = query.getInstanceStatuses();
        if (CollUtil.isEmpty(statuses)) {
            query.setInstanceStatuses(null);
            return;
        }
        boolean invalidStatus = statuses.stream().anyMatch(status -> !INSTANCE_STATUSES.contains(status));
        if (invalidStatus) {
            throw new BizException("流程实例状态仅支持审批中、审批通过、审批拒绝或已取消");
        }
        query.setInstanceStatuses(statuses.stream().distinct().toList());
    }

    private FlowInstanceStatusEnum getInstanceStatus(Integer code) {
        return Arrays.stream(FlowInstanceStatusEnum.values())
                .filter(status -> Objects.equals(status.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    private void requireNodeOperation(FlowNode root, FlowTask task,
                                      Function<FlowNode, Boolean> permission, String operation) {
        FlowNode node = FlowParser.findNodeByKey(root, task.getNodeKey());
        if (node == null) {
            throw new BizException("审批节点不存在");
        }
        if (Boolean.FALSE.equals(permission.apply(node))) {
            throw new BizException("当前审批节点不允许" + operation);
        }
    }

    private FlowNode getReturnModel(FlowInstance instance) {
        FlowDefinition definition = flowDefinitionService.getById(instance.getDefinitionId());
        if (definition == null || !Objects.equals(definition.getOrgId(), instance.getOrgId())) {
            throw new BizException("流程发布定义不存在");
        }
        FlowNode root = definition.getModelNode();
        FlowParser.makeParentNode(root);
        return root;
    }

    private void invalidateTasks(List<FlowTask> tasks, Long returnTaskId, LocalDateTime now) {
        if (tasks.isEmpty()) {
            return;
        }
        List<Long> ids = tasks.stream().map(FlowTask::getId).toList();
        List<FlowTask> cancelled = runningApprovalTasks(tasks.getFirst().getInstanceId()).stream()
                .filter(task -> ids.contains(task.getId())).toList();
        flowTaskDAO.update(new LambdaUpdateWrapper<FlowTask>().in(FlowTask::getId, ids)
                .isNull(FlowTask::getReturnTaskId)
                .set(FlowTask::getReturnTaskId, returnTaskId)
                .set(FlowTask::getNextRemindTime, null));
        flowTaskDAO.update(new LambdaUpdateWrapper<FlowTask>().in(FlowTask::getId, ids)
                .in(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode(), FlowTaskStatusEnum.WAITING_ADD_SIGN.getCode())
                .set(FlowTask::getStatus, FlowTaskStatusEnum.CANCEL.getCode())
                .set(FlowTask::getEndTime, now).set(FlowTask::getComment, "因流程退回取消待办"));
        publishCancelledTasks(cancelled, FlowTaskCancelReason.RETURN);
        flowTimeoutNoticeDAO.update(new LambdaUpdateWrapper<FlowTimeoutNotice>()
                .in(FlowTimeoutNotice::getTaskId, ids)
                .eq(FlowTimeoutNotice::getEventType, FlowTimeoutNoticeEnums.EventType.REMINDER.getCode())
                .eq(FlowTimeoutNotice::getState, FlowTimeoutNoticeEnums.State.PENDING.getCode())
                .set(FlowTimeoutNotice::getState, FlowTimeoutNoticeEnums.State.CANCELLED.getCode()));
    }

    private FlowTask getExecutableTask(Long taskId, LoginUser loginUser) {
        FlowTask task = flowTaskDAO.selectById(taskId);
        if (task == null
                || !Objects.equals(task.getOrgId(), loginUser.getOrgId())
                || !Objects.equals(task.getAssignee(), loginUser.getId())
                || !Objects.equals(task.getNodeType(), FlowNodeEnum.Type.APPROVE.getCode())) {
            throw new BizException("任务不存在或无权处理");
        }
        if (!FlowReturnPath.isActive(task)
                || !Objects.equals(task.getStatus(), FlowTaskStatusEnum.RUNNING.getCode())
                || !Objects.equals(task.getIsDelete(), CommonConstant.IS_NOT_DEL)) {
            throw new BizException(Boolean.TRUE.equals(task.getTimeoutHandled())
                    ? "任务已超时自动处理，请刷新后查看" : "任务已经处理过，请勿重复处理");
        }
        return task;
    }

    private void validateRunningInstance(FlowInstance instance, LoginUser loginUser, FlowTask task) {
        if (instance == null
                || !Objects.equals(instance.getOrgId(), loginUser.getOrgId())
                || !Objects.equals(instance.getOrgId(), task.getOrgId())
                || !Objects.equals(instance.getStatus(), FlowInstanceStatusEnum.RUNNING.getCode())) {
            throw new BizException("流程实例已结束，不能继续审批");
        }
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

    private void initializeTimeout(FlowTask task, FlowTimeoutRule rule) {
        task.setTimeoutRule(rule);
        task.setRemindCount(0);
        task.setTimeoutHandled(false);
        task.setDeadline(rule != null && Boolean.TRUE.equals(rule.getAutoEnabled())
                ? task.getStartTime().plusMinutes(FlowTimeoutRule.minutes(rule.getAutoAfter(), rule.getAutoUnit())) : null);
        task.setNextRemindTime(rule != null && Boolean.TRUE.equals(rule.getReminderEnabled())
                ? task.getStartTime().plusMinutes(FlowTimeoutRule.minutes(rule.getFirstReminder(), rule.getFirstReminderUnit())) : null);
    }

}
