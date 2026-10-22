package com.plasticene.boot.flow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.model.dto.FlowExtraSetting;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;
import com.plasticene.boot.flow.core.model.query.FlowTaskQuery;
import com.plasticene.boot.flow.core.model.vo.FlowTaskPageVO;
import com.plasticene.boot.flow.core.provider.FlowOrganizationProvider;
import com.plasticene.boot.flow.core.provider.FlowTaskAssigneeProvider;
import com.plasticene.boot.flow.core.service.CategoryService;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 流程任务服务测试
 *
 * @author ZFJ
 * @since 2026-09-15
 */
@ExtendWith(MockitoExtension.class)
class FlowTaskServiceImplTest {

    @Mock
    private FlowTaskDAO flowTaskDAO;
    @Mock
    private FlowTaskAssigneeProvider flowTaskAssigneeProvider;
    @Mock
    private FlowRuntimeService flowRuntimeService;
    @Mock
    private ProcessExecutor processExecutor;
    @Mock
    private FlowDefinitionService flowDefinitionService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private FlowOrganizationProvider flowOrganizationProvider;
    @InjectMocks
    private FlowTaskServiceImpl service;

    @BeforeEach
    void setUpLoginUser() {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(8L);
        loginUser.setOrgId(9L);
        LoginUserHolder.set(loginUser);
    }

    @AfterEach
    void clearLoginUser() {
        LoginUserHolder.remove();
    }

    @Test
    void shouldRejectApprovalAfterInstanceWasCancelled() {
        FlowTaskParam param = new FlowTaskParam();
        param.setTaskId(11L);
        when(flowTaskDAO.selectById(11L)).thenReturn(runningTask());
        when(flowRuntimeService.selectInstanceForUpdate(21L)).thenReturn(cancelledInstance());

        assertThrows(BizException.class, () -> service.approveTask(param));
    }

    @Test
    void shouldRejectRejectionAfterInstanceWasCancelled() {
        FlowTaskParam param = new FlowTaskParam();
        param.setTaskId(11L);
        when(flowTaskDAO.selectById(11L)).thenReturn(runningTask());
        when(flowRuntimeService.selectInstanceForUpdate(21L)).thenReturn(cancelledInstance());

        assertThrows(BizException.class, () -> service.rejectTask(param));
    }

    @Test
    void shouldKeepCancelledTaskButHideLogicallyDeletedRunningTaskInHistory() {
        FlowTask completed = task(1L, CommonConstant.IS_NOT_DEL, FlowTaskStatusEnum.COMPLETE);
        FlowTask cancelled = task(2L, CommonConstant.IS_NOT_DEL, FlowTaskStatusEnum.CANCEL);
        FlowTask invalidRunning = task(3L, CommonConstant.IS_DEL, FlowTaskStatusEnum.RUNNING);
        when(flowTaskDAO.selectList(any())).thenReturn(List.of(completed, cancelled, invalidRunning));

        List<FlowTask> history = service.listTaskByInstanceId(21L);

        assertEquals(List.of(completed, cancelled), history);
    }

    @Test
    void shouldIncludeCancelledTaskInDonePageAndCalculateElapsedSeconds() {
        FlowTaskQuery query = new FlowTaskQuery();
        query.setKeyword(" 采购 ");
        query.setCategory(" purchase ");
        FlowTaskPageVO vo = new FlowTaskPageVO();
        vo.setTaskId(31L);
        vo.setCategory("purchase");
        vo.setStartUserId(7L);
        vo.setTaskStartTime(LocalDateTime.of(2026, 9, 16, 10, 0));
        vo.setTaskEndTime(LocalDateTime.of(2026, 9, 16, 10, 1, 30));
        vo.setStatus(FlowTaskStatusEnum.CANCEL.getCode());
        Page<FlowTaskPageVO> page = new Page<>(1, 10);
        page.setRecords(List.of(vo));
        page.setTotal(1);
        when(flowTaskDAO.pageTask(any(), eq(query))).thenReturn(page);
        when(categoryService.getCategoryMap()).thenReturn(Map.of("purchase", "采购管理"));
        when(flowOrganizationProvider.getUserMap()).thenReturn(Map.of(7L, "张敏"));

        PageResult<FlowTaskPageVO> result = service.pageMyDone(query);

        assertEquals("采购", query.getKeyword());
        assertEquals("purchase", query.getCategory());
        assertEquals(8L, query.getAssignee());
        assertEquals(9L, query.getOrgId());
        assertEquals(1L, result.getTotal());
        assertEquals("采购管理", result.getList().getFirst().getCategoryName());
        assertEquals("张敏", result.getList().getFirst().getStartUserName());
        assertEquals(90L, result.getList().getFirst().getElapsedTime());
        assertEquals(List.of(
                FlowTaskStatusEnum.COMPLETE.getCode(),
                FlowTaskStatusEnum.REJECT.getCode(),
                FlowTaskStatusEnum.CANCEL.getCode()
        ), query.getStatuses());
        verify(flowTaskDAO).pageTask(any(), eq(query));
    }

    @Test
    void shouldRejectInvalidTaskStartTimeRange() {
        FlowTaskQuery query = new FlowTaskQuery();
        query.setTaskStartTimeBegin(LocalDateTime.of(2026, 9, 16, 11, 0));
        query.setTaskStartTimeEnd(LocalDateTime.of(2026, 9, 16, 10, 0));

        assertThrows(BizException.class, () -> service.pageMyTodo(query));
        verify(flowTaskDAO, never()).pageTask(any(), any());
    }

    @Test
    void shouldFilterDonePageByRequestedStatuses() {
        FlowTaskQuery query = new FlowTaskQuery();
        query.setStatuses(List.of(
                FlowTaskStatusEnum.REJECT.getCode(),
                FlowTaskStatusEnum.CANCEL.getCode(),
                FlowTaskStatusEnum.REJECT.getCode()
        ));
        Page<FlowTaskPageVO> page = new Page<>(1, 10);
        page.setRecords(List.of());
        when(flowTaskDAO.pageTask(any(), eq(query))).thenReturn(page);

        service.pageMyDone(query);

        assertEquals(List.of(
                FlowTaskStatusEnum.REJECT.getCode(),
                FlowTaskStatusEnum.CANCEL.getCode()
        ), query.getStatuses());
    }

    @Test
    void shouldRejectRunningStatusForDonePage() {
        FlowTaskQuery query = new FlowTaskQuery();
        query.setStatuses(List.of(FlowTaskStatusEnum.RUNNING.getCode()));

        assertThrows(BizException.class, () -> service.pageMyDone(query));
        verify(flowTaskDAO, never()).pageTask(any(), any());
    }

    @Test
    void shouldPageCopyTasksAndFillInstanceStatus() {
        FlowTaskQuery query = new FlowTaskQuery();
        query.setInstanceStatuses(List.of(
                FlowInstanceStatusEnum.RUNNING.getCode(),
                FlowInstanceStatusEnum.RUNNING.getCode(),
                FlowInstanceStatusEnum.APPROVE.getCode()
        ));
        FlowTaskPageVO vo = new FlowTaskPageVO();
        vo.setTaskId(31L);
        vo.setCategory("purchase");
        vo.setStartUserId(7L);
        vo.setTaskStartTime(LocalDateTime.of(2026, 9, 16, 10, 0));
        vo.setTaskEndTime(LocalDateTime.of(2026, 9, 16, 10, 0));
        vo.setInstanceStatus(FlowInstanceStatusEnum.RUNNING.getCode());
        Page<FlowTaskPageVO> page = new Page<>(1, 10);
        page.setRecords(List.of(vo));
        page.setTotal(1);
        when(flowTaskDAO.pageTask(any(), eq(query))).thenReturn(page);
        when(categoryService.getCategoryMap()).thenReturn(Map.of("purchase", "采购管理"));
        when(flowOrganizationProvider.getUserMap()).thenReturn(Map.of(7L, "张敏"));

        PageResult<FlowTaskPageVO> result = service.pageMyCopy(query);

        assertEquals(FlowNodeEnum.Type.COPY.getCode(), query.getNodeType());
        assertEquals(List.of(FlowTaskStatusEnum.COMPLETE.getCode()), query.getStatuses());
        assertEquals(List.of(
                FlowInstanceStatusEnum.RUNNING.getCode(),
                FlowInstanceStatusEnum.APPROVE.getCode()
        ), query.getInstanceStatuses());
        assertEquals(8L, query.getAssignee());
        assertEquals(9L, query.getOrgId());
        assertEquals("审批中", result.getList().getFirst().getInstanceStatusName());
        assertEquals("张敏", result.getList().getFirst().getStartUserName());
    }

    @Test
    void shouldRejectInvalidInstanceStatusForCopyPage() {
        FlowTaskQuery query = new FlowTaskQuery();
        query.setInstanceStatuses(List.of(99));

        assertThrows(BizException.class, () -> service.pageMyCopy(query));

        verify(flowTaskDAO, never()).pageTask(any(), any());
    }

    @Test
    void shouldCreateOneCopyTaskPerDistinctAssignee() {
        FlowInstance instance = new FlowInstance();
        instance.setId(21L);
        instance.setOrgId(9L);
        FlowNode copyNode = new FlowNode();
        copyNode.setKey("copy");
        copyNode.setName("财务抄送");
        copyNode.setType(FlowNodeEnum.Type.COPY.getCode());
        when(flowTaskAssigneeProvider.getAssignees(copyNode)).thenReturn(List.of(8L, 8L, 10L));

        service.createCopyTask(instance, copyNode);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<FlowTask>> taskCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(flowTaskDAO).insert(taskCaptor.capture());
        List<FlowTask> tasks = taskCaptor.getValue().stream().toList();
        assertEquals(2, tasks.size());
        assertEquals(List.of(8L, 10L), tasks.stream().map(FlowTask::getAssignee).toList());
        assertTrue(tasks.stream().allMatch(task -> task.getNodeType().equals(FlowNodeEnum.Type.COPY.getCode())));
        assertTrue(tasks.stream().allMatch(task -> task.getStatus().equals(FlowTaskStatusEnum.COMPLETE.getCode())));
    }

    @Test
    void shouldRejectApprovalForAnotherAssignee() {
        FlowTask task = runningTask();
        task.setAssignee(99L);
        when(flowTaskDAO.selectById(11L)).thenReturn(task);
        FlowTaskParam param = new FlowTaskParam();
        param.setTaskId(11L);

        assertThrows(BizException.class, () -> service.approveTask(param));
        verify(flowRuntimeService, never()).selectInstanceForUpdate(any());
    }

    @Test
    void shouldAutoApproveRepeatedAssigneeOncePerProcess() {
        FlowInstance instance = runningInstance();
        FlowNode node = approveNode("second");
        when(flowTaskAssigneeProvider.getAssignees(node)).thenReturn(List.of(8L));
        when(flowDefinitionService.getById(31L)).thenReturn(definition(
                FlowExtraSetting.ApproverDeduplication.ONCE_PER_PROCESS, false));
        FlowTask previous = completedApproval(1L, "first", 8L,
                FlowNodeEnum.ApproveType.MANUAL.getCode(), LocalDateTime.now().minusMinutes(2));
        when(flowTaskDAO.selectList(any())).thenReturn(List.of(previous));

        service.createApproveTask(instance, node);

        ArgumentCaptor<FlowTask> captor = ArgumentCaptor.forClass(FlowTask.class);
        verify(flowTaskDAO).insert(captor.capture());
        assertEquals(8L, captor.getValue().getAssignee());
        assertEquals(FlowNodeEnum.ApproveType.AUTO_PASS.getCode(), captor.getValue().getApproveType());
        assertEquals(FlowTaskStatusEnum.COMPLETE.getCode(), captor.getValue().getStatus());
        verify(processExecutor).moveToNextNode(instance, node);
    }

    @Test
    void shouldNotAutoApproveWhenPreviousNodeHasDifferentAssignee() {
        FlowInstance instance = runningInstance();
        FlowNode node = approveNode("second");
        when(flowTaskAssigneeProvider.getAssignees(node)).thenReturn(List.of(8L));
        when(flowDefinitionService.getById(31L)).thenReturn(definition(
                FlowExtraSetting.ApproverDeduplication.CONSECUTIVE, false));
        FlowTask earlier = completedApproval(1L, "first", 8L,
                FlowNodeEnum.ApproveType.MANUAL.getCode(), LocalDateTime.now().minusMinutes(3));
        FlowTask previous = completedApproval(2L, "middle", 9L,
                FlowNodeEnum.ApproveType.MANUAL.getCode(), LocalDateTime.now().minusMinutes(2));
        when(flowTaskDAO.selectList(any())).thenReturn(List.of(earlier, previous));

        service.createApproveTask(instance, node);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<FlowTask>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(flowTaskDAO).insert(captor.capture());
        assertEquals(FlowTaskStatusEnum.RUNNING.getCode(), captor.getValue().iterator().next().getStatus());
        verify(processExecutor, never()).moveToNextNode(any(), any());
    }

    @Test
    void shouldAutoApproveSameAssigneeOnConsecutiveApproveNode() {
        FlowInstance instance = runningInstance();
        FlowNode previousNode = approveNode("first");
        FlowNode node = approveNode("second");
        node.setParentNode(previousNode);
        when(flowTaskAssigneeProvider.getAssignees(node)).thenReturn(List.of(8L));
        when(flowDefinitionService.getById(31L)).thenReturn(definition(
                FlowExtraSetting.ApproverDeduplication.CONSECUTIVE, false));
        FlowTask previous = completedApproval(1L, "first", 8L,
                FlowNodeEnum.ApproveType.MANUAL.getCode(), LocalDateTime.now().minusMinutes(2));
        when(flowTaskDAO.selectList(any())).thenReturn(List.of(previous));

        service.createApproveTask(instance, node);

        ArgumentCaptor<FlowTask> captor = ArgumentCaptor.forClass(FlowTask.class);
        verify(flowTaskDAO).insert(captor.capture());
        assertEquals(FlowNodeEnum.ApproveType.AUTO_PASS.getCode(), captor.getValue().getApproveType());
        verify(processExecutor).moveToNextNode(instance, node);
    }

    @Test
    void shouldAllowWithdrawWhenNextApproverHasNotHandledTask() {
        LocalDateTime approvedAt = LocalDateTime.now().minusMinutes(1);
        FlowTask approved = completedApproval(11L, "first", 8L,
                FlowNodeEnum.ApproveType.MANUAL.getCode(), approvedAt);
        approved.setOrgId(9L);
        FlowTask pending = task(12L, CommonConstant.IS_NOT_DEL, FlowTaskStatusEnum.RUNNING);
        pending.setOrgId(9L);
        pending.setNodeKey("second");
        pending.setNodeType(FlowNodeEnum.Type.APPROVE.getCode());
        FlowInstance instance = runningInstance();
        instance.setCurrentNodeKey("second");
        FlowDefinition definition = definition(FlowExtraSetting.ApproverDeduplication.NONE, true);

        assertTrue(service.canWithdrawTask(approved, instance, definition, List.of(approved, pending)));
    }

    @Test
    void shouldAllowAssignedAutoPassTaskToWithdraw() {
        FlowTask approved = completedApproval(11L, "first", 8L,
                FlowNodeEnum.ApproveType.AUTO_PASS.getCode(), LocalDateTime.now().minusMinutes(1));
        approved.setOrgId(9L);
        FlowTask pending = task(12L, CommonConstant.IS_NOT_DEL, FlowTaskStatusEnum.RUNNING);
        pending.setOrgId(9L);
        pending.setNodeKey("second");
        pending.setNodeType(FlowNodeEnum.Type.APPROVE.getCode());
        FlowInstance instance = runningInstance();
        instance.setCurrentNodeKey("second");
        FlowDefinition definition = definition(FlowExtraSetting.ApproverDeduplication.NONE, true);

        assertTrue(service.canWithdrawTask(approved, instance, definition, List.of(approved, pending)));
    }

    @Test
    void shouldWithdrawApprovalAndRestoreOriginalTask() {
        FlowTask approved = completedApproval(11L, "first", 8L,
                FlowNodeEnum.ApproveType.MANUAL.getCode(), LocalDateTime.now().minusMinutes(1));
        approved.setOrgId(9L);
        approved.setNodeName("一级审批");
        approved.setApproveMode(FlowNodeEnum.ApproveMode.ALL.getCode());
        FlowTask pending = task(12L, CommonConstant.IS_NOT_DEL, FlowTaskStatusEnum.RUNNING);
        pending.setOrgId(9L);
        pending.setNodeKey("second");
        pending.setNodeType(FlowNodeEnum.Type.APPROVE.getCode());
        FlowInstance instance = runningInstance();
        instance.setCurrentNodeKey("second");
        FlowDefinition definition = definition(FlowExtraSetting.ApproverDeduplication.NONE, true);
        when(flowTaskDAO.selectById(11L)).thenReturn(approved);
        when(flowRuntimeService.selectInstanceForUpdate(21L)).thenReturn(instance);
        when(flowDefinitionService.getById(31L)).thenReturn(definition);
        when(flowTaskDAO.selectList(any())).thenReturn(List.of(approved, pending));
        when(flowTaskDAO.update(any(LambdaUpdateWrapper.class))).thenReturn(1);

        service.withdrawTask(11L);

        verify(flowTaskDAO, times(2)).update(any(LambdaUpdateWrapper.class));
        verify(flowRuntimeService).updateInstanceCurrentNode(eq(21L), any(FlowNode.class));
    }

    @Test
    void shouldRejectWithdrawAfterLaterApprovalWasHandled() {
        LocalDateTime approvedAt = LocalDateTime.now().minusMinutes(2);
        FlowTask approved = completedApproval(11L, "first", 8L,
                FlowNodeEnum.ApproveType.MANUAL.getCode(), approvedAt);
        approved.setOrgId(9L);
        FlowTask later = completedApproval(12L, "second", 9L,
                FlowNodeEnum.ApproveType.MANUAL.getCode(), approvedAt.plusMinutes(1));
        later.setOrgId(9L);
        FlowInstance instance = runningInstance();
        instance.setCurrentNodeKey("third");
        FlowDefinition definition = definition(FlowExtraSetting.ApproverDeduplication.NONE, true);

        assertFalse(service.canWithdrawTask(approved, instance, definition, List.of(approved, later)));
    }

    @Test
    void shouldKeepOtherParallelBranchesRunningWhenAnyApproverCompletes() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(
                        new com.baomidou.mybatisplus.core.MybatisConfiguration(), "test"), FlowTask.class);
        FlowTask task = runningTask();
        task.setApproveMode(FlowNodeEnum.ApproveMode.ANY.getCode());
        FlowInstance instance = runningInstance();
        when(flowTaskDAO.selectById(11L)).thenReturn(task);
        when(flowRuntimeService.selectInstanceForUpdate(21L)).thenReturn(instance);
        when(flowTaskDAO.update(any(FlowTask.class), any())).thenReturn(1);
        FlowDefinition definition = new FlowDefinition();
        FlowNode approve = approveNode("approve");
        definition.setModelNode(approve);
        when(flowDefinitionService.getById(31L)).thenReturn(definition);
        FlowTaskParam param = new FlowTaskParam();
        param.setTaskId(11L);
        service.approveTask(param);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<FlowTask>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(flowTaskDAO).update(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("node_key"));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue("approve"));
        verify(processExecutor).moveToNextNode(instance, approve);
    }

    private FlowTask runningTask() {
        FlowTask task = new FlowTask();
        task.setId(11L);
        task.setOrgId(9L);
        task.setInstanceId(21L);
        task.setNodeKey("approve");
        task.setNodeType(FlowNodeEnum.Type.APPROVE.getCode());
        task.setAssignee(8L);
        task.setStatus(FlowTaskStatusEnum.RUNNING.getCode());
        task.setIsDelete(CommonConstant.IS_NOT_DEL);
        return task;
    }

    private FlowInstance runningInstance() {
        FlowInstance instance = new FlowInstance();
        instance.setId(21L);
        instance.setOrgId(9L);
        instance.setUserId(7L);
        instance.setDefinitionId(31L);
        instance.setStatus(FlowInstanceStatusEnum.RUNNING.getCode());
        return instance;
    }

    private FlowNode approveNode(String key) {
        FlowNode node = new FlowNode();
        node.setKey(key);
        node.setName(key);
        node.setType(FlowNodeEnum.Type.APPROVE.getCode());
        node.setApproveType(FlowNodeEnum.ApproveType.MANUAL.getCode());
        node.setApproveMode(FlowNodeEnum.ApproveMode.ALL.getCode());
        node.setSelfApprove(FlowNodeEnum.SelfApprove.MANUAL.getCode());
        return node;
    }

    private FlowDefinition definition(FlowExtraSetting.ApproverDeduplication strategy,
                                      boolean allowWithdraw) {
        FlowDefinition definition = new FlowDefinition();
        FlowExtraSetting setting = new FlowExtraSetting();
        setting.setApproverDeduplication(strategy);
        setting.setAllowApproverWithdraw(allowWithdraw);
        definition.setExtraSetting(setting);
        return definition;
    }

    private FlowTask completedApproval(Long id, String nodeKey, Long assignee,
                                       Integer approveType, LocalDateTime endTime) {
        FlowTask task = task(id, CommonConstant.IS_NOT_DEL, FlowTaskStatusEnum.COMPLETE);
        task.setNodeKey(nodeKey);
        task.setNodeType(FlowNodeEnum.Type.APPROVE.getCode());
        task.setAssignee(assignee);
        task.setApproveType(approveType);
        task.setEndTime(endTime);
        return task;
    }

    private FlowInstance cancelledInstance() {
        FlowInstance instance = new FlowInstance();
        instance.setId(21L);
        instance.setOrgId(9L);
        instance.setStatus(FlowInstanceStatusEnum.CANCEL.getCode());
        return instance;
    }

    private FlowTask task(Long id, Integer isDelete, FlowTaskStatusEnum status) {
        FlowTask task = new FlowTask();
        task.setId(id);
        task.setInstanceId(21L);
        task.setIsDelete(isDelete);
        task.setStatus(status.getCode());
        return task;
    }
}
