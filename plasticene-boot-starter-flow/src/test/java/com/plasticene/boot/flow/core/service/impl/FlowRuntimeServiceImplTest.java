package com.plasticene.boot.flow.core.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.dao.FlowInstanceDAO;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.event.InstanceEvent;
import com.plasticene.boot.flow.core.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.model.dto.FlowInstanceNodeTimeDTO;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.query.FlowInstanceQuery;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceDetailVO;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceStatisticsVO;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceVO;
import com.plasticene.boot.flow.core.provider.FlowOrganizationProvider;
import com.plasticene.boot.flow.core.service.CategoryService;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

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
    private ApplicationContext applicationContext;
    @Mock
    private FlowDefinitionService flowDefinitionService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private FlowTaskService flowTaskService;
    @Mock
    private FlowOrganizationProvider flowOrganizationProvider;
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
        when(categoryService.getCategoryMap()).thenReturn(Map.of("admin", "行政审批"));
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
        when(categoryService.getCategoryMap()).thenReturn(Map.of());

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
        when(categoryService.getCategoryMap()).thenReturn(Map.of());
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

    @Test
    void shouldCancelOwnRunningApplication() {
        FlowInstance instance = runningInstance(8L);
        FlowNode currentNode = new FlowNode();
        currentNode.setKey("approve");
        currentNode.setName("负责人审批");
        FlowDefinition definition = new FlowDefinition();
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
        ArgumentCaptor<InstanceEvent> eventCaptor = ArgumentCaptor.forClass(InstanceEvent.class);
        verify(applicationContext).publishEvent(eventCaptor.capture());
        assertEquals(FlowInstanceStatusEnum.CANCEL.getCode(), eventCaptor.getValue().getInstance().getStatus());
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
    void shouldReturnOwnApplicationDetailWithRouteAndTasks() {
        FlowInstance instance = runningInstance(8L);
        instance.setCategory("purchase");
        instance.setVarMap(Map.of("amount", 68000));
        FlowDefinition definition = new FlowDefinition();
        definition.setId(31L);
        definition.setOrgId(10L);
        definition.setName("办公设备购买");
        definition.setCode("buy_091401");
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
        when(categoryService.getCategoryMap()).thenReturn(Map.of("purchase", "采购"));
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
        when(categoryService.getCategoryMap()).thenReturn(Map.of());
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
