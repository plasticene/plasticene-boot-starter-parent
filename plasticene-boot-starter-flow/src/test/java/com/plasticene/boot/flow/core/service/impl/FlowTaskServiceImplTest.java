package com.plasticene.boot.flow.core.service.impl;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.flow.core.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;
import com.plasticene.boot.flow.core.provider.FlowTaskAssigneeProvider;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    @InjectMocks
    private FlowTaskServiceImpl service;

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
        FlowTask cancelled = task(2L, CommonConstant.IS_DEL, FlowTaskStatusEnum.CANCEL);
        FlowTask invalidRunning = task(3L, CommonConstant.IS_DEL, FlowTaskStatusEnum.RUNNING);
        when(flowTaskDAO.selectList(any())).thenReturn(List.of(completed, cancelled, invalidRunning));

        List<FlowTask> history = service.listTaskByInstanceId(21L);

        assertEquals(List.of(completed, cancelled), history);
    }

    private FlowTask runningTask() {
        FlowTask task = new FlowTask();
        task.setId(11L);
        task.setInstanceId(21L);
        task.setNodeKey("approve");
        return task;
    }

    private FlowInstance cancelledInstance() {
        FlowInstance instance = new FlowInstance();
        instance.setId(21L);
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
