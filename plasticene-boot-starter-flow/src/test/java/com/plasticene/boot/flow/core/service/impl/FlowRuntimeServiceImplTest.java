package com.plasticene.boot.flow.core.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.dao.FlowInstanceDAO;
import com.plasticene.boot.flow.core.dao.FlowTimeoutNoticeDAO;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.event.FlowInstanceEvent;
import com.plasticene.boot.flow.core.event.FlowEventPublisher;
import com.plasticene.boot.flow.core.event.FlowEventType;
import com.plasticene.boot.flow.core.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.model.dto.FlowExtraSetting;
import com.plasticene.boot.flow.core.model.dto.FlowInstanceNodeTimeDTO;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.query.FlowInstanceQuery;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceDetailVO;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceStatisticsVO;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceVO;
import com.plasticene.boot.flow.core.provider.FlowOrganizationProvider;
import com.plasticene.boot.flow.core.service.FlowCategoryService;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 流程实例运行时服务测试
 *
 * @author ZFJ
 * @since 2026-09-15
 */
@ExtendWith(MockitoExtension.class)
class FlowRuntimeServiceImplTest {

    @Mock
    private FlowInstanceDAO flowInstanceDAO;
    @Mock
    private ProcessExecutor processExecutor;
    @Mock
    private FlowEventPublisher flowEventPublisher;
    @Mock
    private FlowDefinitionService flowDefinitionService;
    @Mock
    private FlowCategoryService flowCategoryService;
    @Mock
    private FlowTaskService flowTaskService;
    @Mock
    private FlowOrganizationProvider flowOrganizationProvider;
    @Mock
    private FlowTimeoutNoticeDAO timeoutNoticeDAO;
    @Mock
    private com.plasticene.boot.flow.core.dao.FlowFormChangeRecordDAO formChangeRecordDAO;
    @InjectMocks
    private FlowRuntimeServiceImpl service;

    @BeforeEach
    void setUpLoginUser() {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(8L);
        loginUser.setOrgId(10L);
        LoginUserHolder.set(loginUser);
    }

    @AfterEach
    void clearLoginUser() {
        LoginUserHolder.remove();
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = FlowInstanceStatusEnum.class,
            names = {"APPROVE", "REJECT", "CANCEL"})
    void shouldPublishDistinctTerminalInstanceEvents(FlowInstanceStatusEnum status) {
        FlowInstance instance = runningInstance(8L);
        FlowNode node = new FlowNode();
        node.setKey("approve");
        node.setName("审批");
        service.endInstance(instance, node, status);
        ArgumentCaptor<FlowInstanceEvent> captor = ArgumentCaptor.forClass(FlowInstanceEvent.class);
        verify(flowEventPublisher).publish(captor.capture());
        FlowEventType expected = switch (status) {
            case APPROVE -> FlowEventType.INSTANCE_APPROVED;
            case REJECT -> FlowEventType.INSTANCE_REJECTED;
            case CANCEL -> FlowEventType.INSTANCE_CANCELLED;
            default -> throw new AssertionError(status);
        };
        assertEquals(expected, captor.getValue().eventType());
        assertEquals(status.getCode(), captor.getValue().status());
        assertEquals(instance.getId(), captor.getValue().instanceId());
    }

    @Test
    void shouldPublishStartedSnapshotBeforeExecutingNodes() {
        FlowDefinition definition = new FlowDefinition();
        definition.setOrgId(10L);
        definition.setCategory("leave");
        FlowNode node = new FlowNode();
        node.setKey("start");
        definition.setModelNode(node);
        when(flowDefinitionService.getStartableDefinition(31L)).thenReturn(definition);
        when(flowInstanceDAO.insert(any(FlowInstance.class))).thenAnswer(call -> {
            call.<FlowInstance>getArgument(0).setId(21L);
            return 1;
        });
        service.startFlowInstanceById(31L, 41L, Map.of());
        ArgumentCaptor<FlowInstanceEvent> captor = ArgumentCaptor.forClass(FlowInstanceEvent.class);
        var order = org.mockito.Mockito.inOrder(flowEventPublisher, processExecutor);
        order.verify(flowEventPublisher).publish(captor.capture());
        order.verify(processExecutor).executeNode(any(), eq(node));
        assertEquals(FlowEventType.INSTANCE_STARTED, captor.getValue().eventType());
        assertEquals(21L, captor.getValue().instanceId());
        assertEquals(41L, captor.getValue().businessId());
        assertEquals(8L, captor.getValue().startUserId());
    }

    @Test
    void shouldFilterHistoricalFieldsUsingCurrentTaskPermissions() {
        var instance = runningInstance(8L);
        FlowDefinition definition = new FlowDefinition();
        definition.setOrgId(10L);
        FlowNode node = new FlowNode();
        node.setKey("approve");
        node.setType(1);
        var hidden = new com.plasticene.boot.flow.core.model.dto.FlowFieldPermission();
        hidden.setField("secret");
        hidden.setPermission(com.plasticene.boot.flow.core.model.dto.FlowFieldPermission.Permission.HIDDEN);
        node.setFieldsPermission(List.of(hidden));
        definition.setModelNode(node);
        definition.setFormConfig(Map.of("rule", List.of(
                Map.of("field", "amount", "title", "金额"), Map.of("field", "secret", "title", "秘密"))));
        FlowTask task = new FlowTask();
        task.setId(41L);
        task.setNodeKey("approve");
        task.setNodeType(1);
        task.setAssignee(8L);
        task.setStatus(0);
        when(flowInstanceDAO.selectById(21L)).thenReturn(instance);
        when(flowDefinitionService.getById(31L)).thenReturn(definition);
        when(flowTaskService.listTaskByInstanceId(21L)).thenReturn(List.of(task));
        when(flowOrganizationProvider.getUserMap()).thenReturn(Map.of(8L, "张三"));
        when(processExecutor.calculateRouteTrace(node, Map.of())).thenReturn(List.of(node));
        var changes = com.plasticene.boot.flow.core.utils.FlowFormChanges.diff(definition.getFormConfig(),
                Map.of(), Map.of("amount", 100, "secret", "private"));
        var mixed = new com.plasticene.boot.flow.core.entity.FlowFormChangeRecord();
        mixed.setId(2L);
        mixed.setFieldDetails(changes);
        var allHidden = new com.plasticene.boot.flow.core.entity.FlowFormChangeRecord();
        allHidden.setId(1L);
        allHidden.setFieldDetails(List.of(changes.getLast()));
        when(formChangeRecordDAO.selectList(any())).thenReturn(List.of(mixed, allHidden));
        var records = service.getInstanceDetail(21L, 41L).getFormChangeRecords();
        assertEquals(1, records.size());
        assertEquals(1, records.getFirst().getFieldDetails().size());
        assertEquals("amount", records.getFirst().getFieldDetails().getFirst().getField());
        assertEquals(2, mixed.getFieldDetails().size());
        hidden.setField("amount");
        assertEquals("secret", service.getInstanceDetail(21L, 41L).getFormChangeRecords()
                .getFirst().getFieldDetails().getFirst().getField());
    }

    @Test
    void shouldSeparateGroupApprovalPermissionFromTransferAndAddSignCapabilities() {
        FlowInstance instance = runningInstance(8L);
        FlowDefinition definition = new FlowDefinition();
        definition.setOrgId(10L);
        FlowNode node = new FlowNode();
        node.setKey("approve"); node.setType(1); node.setApproveType(0);
        definition.setModelNode(node);
        FlowTask task = new FlowTask();
        task.setId(41L); task.setNodeKey("approve"); task.setNodeType(1);
        task.setAssignee(8L); task.setStatus(0); task.setAddSignId(51L);
        FlowTask waiting = new FlowTask();
        waiting.setId(40L); waiting.setNodeKey("approve"); waiting.setNodeType(1);
        waiting.setAssignee(8L); waiting.setStatus(6);
        when(flowInstanceDAO.selectById(21L)).thenReturn(instance);
        when(flowDefinitionService.getById(31L)).thenReturn(definition);
        when(flowTaskService.listTaskByInstanceId(21L)).thenReturn(List.of(waiting, task));
        when(flowTaskService.isAddSignGroupTask(any())).thenAnswer(call -> call.<FlowTask>getArgument(0).getAddSignId() != null);
        when(flowOrganizationProvider.getUserMap()).thenReturn(Map.of(8L, "加签人"));
        when(processExecutor.calculateRouteTrace(node, Map.of())).thenReturn(List.of(node));
        var detail = service.getInstanceDetail(21L);
        var child = detail.getTasks().getLast();
        assertTrue(child.getProcessable());
        assertFalse(child.getTransferable());
        assertFalse(child.getAddSignable());
        assertFalse(child.getReturnable());
        assertFalse(detail.getTasks().getFirst().getProcessable());
        assertEquals("等待加签", detail.getTasks().getFirst().getStatusName());
    }

    @Test
    void shouldPageCurrentUsersApplicationsAndFillDisplayFields() {
        FlowInstanceQuery query = new FlowInstanceQuery();
        query.setKeyword(" 采购 ");
        FlowInstanceVO vo = new FlowInstanceVO();
        vo.setId(21L);
        vo.setStartUserId(8L);
        vo.setCategory("admin");
        vo.setStatus(FlowInstanceStatusEnum.RUNNING.getCode());
        vo.setStartTime(LocalDateTime.now().minusSeconds(60));
        Page<FlowInstanceVO> page = new Page<>(1, 10);
        page.setRecords(List.of(vo));
        page.setTotal(1);
        when(flowInstanceDAO.pageInstance(any(), eq(query), eq(true))).thenReturn(page);
        when(flowCategoryService.getCategoryMap()).thenReturn(Map.of("admin", "行政审批"));
        when(flowOrganizationProvider.getUserMap()).thenReturn(Map.of(8L, "张三"));

        PageResult<FlowInstanceVO> result = service.page(query);

        assertEquals("采购", query.getKeyword());
        assertEquals(1L, result.getTotal());
        assertEquals("行政审批", result.getList().getFirst().getCategoryName());
        assertEquals("张三", result.getList().getFirst().getStartUserName());
        assertEquals("审批中", result.getList().getFirst().getStatusName());
        assertTrue(result.getList().getFirst().getElapsedTime() >= 60);
        assertTrue(result.getList().getFirst().getCancelable());
    }

    @Test
    void shouldMarkFinishedApplicationAsNotCancelable() {
        FlowInstanceQuery query = new FlowInstanceQuery();
        FlowInstanceVO vo = new FlowInstanceVO();
        vo.setCategory("removed-category");
        vo.setStatus(FlowInstanceStatusEnum.APPROVE.getCode());
        Page<FlowInstanceVO> page = new Page<>(1, 10);
        page.setRecords(List.of(vo));
        page.setTotal(1);
        when(flowInstanceDAO.pageInstance(any(), eq(query), eq(true))).thenReturn(page);
        when(flowCategoryService.getCategoryMap()).thenReturn(Map.of());

        PageResult<FlowInstanceVO> result = service.page(query);

        assertEquals("removed-category", result.getList().getFirst().getCategoryName());
        assertEquals("审批通过", result.getList().getFirst().getStatusName());
        assertFalse(result.getList().getFirst().getCancelable());
    }

    @Test
    void shouldCalculateInstanceAndCurrentNodeElapsedTimeInSeconds() {
        FlowInstanceQuery query = new FlowInstanceQuery();
        query.setOrgId(10L);
        LocalDateTime startTime = LocalDateTime.of(2026, 9, 17, 9, 0);
        FlowInstanceVO vo = new FlowInstanceVO();
        vo.setId(21L);
        vo.setStartUserId(9L);
        vo.setStatus(FlowInstanceStatusEnum.APPROVE.getCode());
        vo.setStartTime(startTime);
        vo.setEndTime(startTime.plusSeconds(600));
        Page<FlowInstanceVO> page = new Page<>(1, 10);
        page.setRecords(List.of(vo));
        page.setTotal(1);
        FlowInstanceNodeTimeDTO nodeTime = new FlowInstanceNodeTimeDTO();
        nodeTime.setInstanceId(21L);
        nodeTime.setStartTime(startTime.plusSeconds(240));
        when(flowInstanceDAO.pageInstance(any(), eq(query), eq(true))).thenReturn(page);
        when(flowInstanceDAO.listCurrentNodeStartTimes(10L, List.of(21L))).thenReturn(List.of(nodeTime));
        when(flowCategoryService.getCategoryMap()).thenReturn(Map.of());
        when(flowOrganizationProvider.getUserMap()).thenReturn(Map.of(9L, "李四"));

        PageResult<FlowInstanceVO> result = service.page(query);

        FlowInstanceVO instance = result.getList().getFirst();
        assertEquals(600L, instance.getElapsedTime());
        assertEquals(360L, instance.getCurrentNodeElapsedTime());
        assertEquals("李四", instance.getStartUserName());
        assertFalse(instance.getCancelable());
    }

    @Test
    void shouldReturnStatisticsWithoutApplyingStatusFilter() {
        FlowInstanceQuery query = new FlowInstanceQuery();
        query.setKeyword(" #21 ");
        query.setCurrentNodeName(" 财务复核 ");
        query.setStatus(FlowInstanceStatusEnum.RUNNING.getCode());
        FlowInstanceStatisticsVO statistics = new FlowInstanceStatisticsVO();
        statistics.setTotal(10L);
        statistics.setRunning(3L);
        statistics.setApproved(5L);
        statistics.setRejected(1L);
        statistics.setCancelled(1L);
        when(flowInstanceDAO.statisticsInstance(query, false)).thenReturn(statistics);

        FlowInstanceStatisticsVO result = service.statistics(query);

        assertEquals(10L, result.getTotal());
        assertEquals(3L, result.getRunning());
        assertEquals("#21", query.getKeyword());
        assertEquals(21L, query.getKeywordInstanceId());
        assertEquals("财务复核", query.getCurrentNodeName());
        verify(flowInstanceDAO).statisticsInstance(query, false);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void shouldCancelOwnRunningApplication(int deleted) {
        FlowInstance instance = runningInstance(8L);
        FlowNode currentNode = new FlowNode();
        currentNode.setKey("approve");
        currentNode.setName("负责人审批");
        FlowDefinition definition = new FlowDefinition();
        definition.setIsDelete(deleted);
        definition.setModelNode(currentNode);
        when(flowInstanceDAO.selectInstanceForUpdate(21L)).thenReturn(instance);
        when(flowDefinitionService.getById(31L)).thenReturn(definition);

        service.cancelInstance(21L);

        verify(flowTaskService).cancelInstanceRunningTask(21L);
        ArgumentCaptor<FlowInstance> updateCaptor = ArgumentCaptor.forClass(FlowInstance.class);
        verify(flowInstanceDAO).updateById(updateCaptor.capture());
        assertEquals(FlowInstanceStatusEnum.CANCEL.getCode(), updateCaptor.getValue().getStatus());
        assertEquals("approve", updateCaptor.getValue().getCurrentNodeKey());
        assertEquals(FlowInstanceStatusEnum.CANCEL.getCode(), instance.getStatus());
        ArgumentCaptor<FlowInstanceEvent> eventCaptor = ArgumentCaptor.forClass(FlowInstanceEvent.class);
        verify(flowEventPublisher).publish(eventCaptor.capture());
        assertEquals(FlowInstanceStatusEnum.CANCEL.getCode(), eventCaptor.getValue().status());
    }

    @Test
    void shouldRejectCancelWhenApplicationBelongsToAnotherUser() {
        when(flowInstanceDAO.selectInstanceForUpdate(21L)).thenReturn(runningInstance(9L));

        assertThrows(BizException.class, () -> service.cancelInstance(21L));

        verify(flowTaskService, never()).cancelInstanceRunningTask(any());
        verify(flowInstanceDAO, never()).updateById(any(FlowInstance.class));
    }

    @Test
    void shouldRejectCancelWhenApplicationHasEnded() {
        FlowInstance instance = runningInstance(8L);
        instance.setStatus(FlowInstanceStatusEnum.APPROVE.getCode());
        when(flowInstanceDAO.selectInstanceForUpdate(21L)).thenReturn(instance);

        assertThrows(BizException.class, () -> service.cancelInstance(21L));

        verify(flowTaskService, never()).cancelInstanceRunningTask(any());
        verify(flowInstanceDAO, never()).updateById(any(FlowInstance.class));
    }

    @Test
    void shouldRejectCancelWhenDefinitionDisablesIt() {
        FlowInstance instance = runningInstance(8L);
        FlowDefinition definition = new FlowDefinition();
        FlowExtraSetting setting = new FlowExtraSetting();
        setting.setAllowSubmitterCancel(false);
        definition.setExtraSetting(setting);
        when(flowInstanceDAO.selectInstanceForUpdate(21L)).thenReturn(instance);
        when(flowDefinitionService.getById(31L)).thenReturn(definition);

        assertThrows(BizException.class, () -> service.cancelInstance(21L));

        verify(flowTaskService, never()).cancelInstanceRunningTask(any());
        verify(flowInstanceDAO, never()).updateById(any(FlowInstance.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void shouldReturnOwnApplicationDetailWithRouteAndTasks(int deleted) {
        FlowInstance instance = runningInstance(8L);
        instance.setCategory("purchase");
        instance.setVarMap(Map.of("amount", 68000));
        FlowDefinition definition = new FlowDefinition();
        definition.setIsDelete(deleted);
        definition.setId(31L);
        definition.setOrgId(10L);
        definition.setName("办公设备购买");
        definition.setCode("buy_091401");
        definition.setFormConfig(Map.of("rule", List.of(Map.of("field", "amount", "title", "金额"))));
        definition.setVersion(2);
        FlowNode startNode = new FlowNode();
        startNode.setKey("start");
        startNode.setName("发起人");
        definition.setModelNode(startNode);
        FlowTask task = new FlowTask();
        task.setId(41L);
        task.setNodeKey("start");
        task.setAssignee(8L);
        task.setStatus(1);
        when(flowInstanceDAO.selectById(21L)).thenReturn(instance);
        when(flowDefinitionService.getById(31L)).thenReturn(definition);
        when(flowCategoryService.getCategoryMap()).thenReturn(Map.of("purchase", "采购"));
        when(flowOrganizationProvider.getUserMap()).thenReturn(Map.of(8L, "张三"));
        when(flowTaskService.listTaskByInstanceId(21L)).thenReturn(List.of(task));
        when(processExecutor.calculateRouteTrace(startNode, instance.getVarMap())).thenReturn(List.of(startNode));

        FlowInstanceDetailVO detail = service.getInstanceDetail(21L);

        assertEquals("办公设备购买", detail.getInstance().getName());
        assertEquals("采购", detail.getInstance().getCategoryName());
        assertEquals("张三", detail.getStartUserName());
        assertEquals(List.of("start"), detail.getRouteNodeKeys());
        assertEquals("张三", detail.getTasks().getFirst().getAssigneeName());
        assertEquals(68000, detail.getFormData().get("amount"));
    }

    @Test
    void shouldExposeOnlyValidRoundCapabilitiesAndSerializeReturnTargetWithoutParentCycles() throws Exception {
        FlowInstance instance = runningInstance(8L);
        FlowDefinition definition = new FlowDefinition();
        definition.setOrgId(10L);
        FlowNode previous = new FlowNode();
        previous.setKey("previous");
        previous.setName("部门审批");
        previous.setType(1);
        previous.setApproveType(0);
        FlowNode current = new FlowNode();
        current.setKey("current");
        current.setType(1);
        current.setApproveType(0);
        previous.setChildNode(current);
        definition.setModelNode(previous);
        FlowTask approved = new FlowTask();
        approved.setId(40L);
        approved.setNodeKey("previous");
        approved.setStatus(1);
        approved.setApproveType(0);
        FlowTask pending = new FlowTask();
        pending.setId(41L);
        pending.setNodeKey("current");
        pending.setNodeType(1);
        pending.setAssignee(8L);
        pending.setStatus(0);
        FlowTask returned = new FlowTask();
        returned.setId(39L);
        returned.setNodeKey("current");
        returned.setNodeType(1);
        returned.setAssignee(8L);
        returned.setStatus(5);
        returned.setReturnTargetNodeKey("previous");
        returned.setReturnTaskId(39L);
        when(flowInstanceDAO.selectById(21L)).thenReturn(instance);
        when(flowDefinitionService.getById(31L)).thenReturn(definition);
        when(flowTaskService.listTaskByInstanceId(21L)).thenReturn(List.of(returned, approved, pending));
        when(flowOrganizationProvider.getUserMap()).thenReturn(Map.of(8L, "李四"));
        when(processExecutor.calculateRouteTrace(previous, Map.of())).thenReturn(List.of(previous, current));
        FlowInstanceDetailVO detail = service.getInstanceDetail(21L);
        assertTrue(detail.getTasks().getLast().getReturnable());
        assertFalse(detail.getTasks().getFirst().getReturnable());
        assertFalse(detail.getTasks().getFirst().getTransferable());
        assertEquals("部门审批", detail.getTasks().getFirst().getReturnTargetNodeName());
        assertEquals(39L, detail.getTasks().getFirst().getReturnTaskId());
        String json = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().writeValueAsString(detail);
        assertFalse(json.contains("parentNode"));
        assertTrue(json.contains("returnable"));

        current.setAllowTransfer(false);
        current.setAllowAddSign(false);
        var restricted = service.getInstanceDetail(21L).getTasks().getLast();
        assertTrue(restricted.getProcessable());
        assertFalse(restricted.getTransferable());
        assertFalse(restricted.getAddSignable());
        assertTrue(restricted.getReturnable());

        current.setAllowReturn(false);
        current.setAllowTransfer(true);
        current.setAllowAddSign(true);
        var independent = service.getInstanceDetail(21L).getTasks().getLast();
        assertTrue(independent.getProcessable());
        assertTrue(independent.getTransferable());
        assertTrue(independent.getAddSignable());
        assertFalse(independent.getReturnable());
    }

    @Test
    void shouldReturnTransferChainAndOnlyAllowCurrentRecipientToProcess() {
        FlowInstance instance = runningInstance(8L);
        FlowDefinition definition = new FlowDefinition();
        definition.setId(31L);
        definition.setOrgId(10L);
        FlowNode node = new FlowNode();
        node.setKey("approve");
        node.setType(1);
        definition.setModelNode(node);
        FlowTask history = new FlowTask();
        history.setId(41L);
        history.setNodeKey("approve");
        history.setNodeType(1);
        history.setAssignee(9L);
        history.setTransferTo(8L);
        history.setStatus(4);
        FlowTask recipient = new FlowTask();
        recipient.setId(42L);
        recipient.setSourceTaskId(41L);
        recipient.setNodeKey("approve");
        recipient.setNodeType(1);
        recipient.setAssignee(8L);
        recipient.setStatus(0);
        when(flowInstanceDAO.selectById(21L)).thenReturn(instance);
        when(flowDefinitionService.getById(31L)).thenReturn(definition);
        when(flowTaskService.listTaskByInstanceId(21L)).thenReturn(List.of(history, recipient));
        when(flowOrganizationProvider.getUserMap()).thenReturn(Map.of(8L, "李四", 9L, "张三"));
        when(processExecutor.calculateRouteTrace(node, Map.of())).thenReturn(List.of(node));
        FlowInstanceDetailVO detail = service.getInstanceDetail(21L);
        assertEquals("已转交", detail.getTasks().getFirst().getStatusName());
        assertEquals("李四", detail.getTasks().getFirst().getTransferToName());
        assertFalse(detail.getTasks().getFirst().getTransferable());
        assertEquals(41L, detail.getTasks().getLast().getSourceTaskId());
        assertTrue(detail.getTasks().getLast().getTransferable());
        LoginUserHolder.get().setId(9L);
        assertFalse(service.getInstanceDetail(21L).getTasks().getLast().getTransferable());
    }

    @Test
    void shouldRejectDetailWhenApplicationBelongsToAnotherOrganization() {
        FlowInstance instance = runningInstance(9L);
        instance.setOrgId(11L);
        when(flowInstanceDAO.selectById(21L)).thenReturn(instance);

        assertThrows(BizException.class, () -> service.getInstanceDetail(21L));

        verify(flowDefinitionService, never()).getById(any());
    }

    @Test
    void shouldReturnDetailForAnotherUserInSameOrganization() {
        FlowInstance instance = runningInstance(9L);
        FlowDefinition definition = new FlowDefinition();
        definition.setId(31L);
        definition.setOrgId(10L);
        definition.setName("采购付款申请");
        definition.setCode("FLOW-PAY-001");
        FlowNode startNode = new FlowNode();
        startNode.setKey("start");
        startNode.setName("发起人");
        definition.setModelNode(startNode);
        when(flowInstanceDAO.selectById(21L)).thenReturn(instance);
        when(flowTaskService.listTaskByInstanceId(21L)).thenReturn(List.of());
        when(flowDefinitionService.getById(31L)).thenReturn(definition);
        when(flowCategoryService.getCategoryMap()).thenReturn(Map.of());
        when(flowOrganizationProvider.getUserMap()).thenReturn(Map.of(8L, "当前用户", 9L, "流程发起人"));
        when(processExecutor.calculateRouteTrace(startNode, Map.of())).thenReturn(List.of(startNode));

        FlowInstanceDetailVO detail = service.getInstanceDetail(21L);

        assertEquals("采购付款申请", detail.getInstance().getName());
        assertEquals("流程发起人", detail.getStartUserName());
        assertTrue(detail.getTasks().isEmpty());
    }

    private FlowInstance runningInstance(Long userId) {
        FlowInstance instance = new FlowInstance();
        instance.setId(21L);
        instance.setOrgId(10L);
        instance.setUserId(userId);
        instance.setDefinitionId(31L);
        instance.setStatus(FlowInstanceStatusEnum.RUNNING.getCode());
        instance.setCurrentNodeKey("approve");
        instance.setCurrentNodeName("负责人审批");
        return instance;
    }
}
