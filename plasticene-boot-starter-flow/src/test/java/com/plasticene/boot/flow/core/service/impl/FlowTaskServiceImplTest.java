package com.plasticene.boot.flow.core.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.executor.ProcessExecutor;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    void shouldRejectApprovalForAnotherAssignee() {
        FlowTask task = runningTask();
        task.setAssignee(99L);
        when(flowTaskDAO.selectById(11L)).thenReturn(task);
        FlowTaskParam param = new FlowTaskParam();
        param.setTaskId(11L);

        assertThrows(BizException.class, () -> service.approveTask(param));
        verify(flowRuntimeService, never()).selectInstanceForUpdate(any());
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
