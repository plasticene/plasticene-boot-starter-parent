package com.plasticene.boot.flow.core.impl.service.task;

import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowInstance;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTask;
import com.plasticene.boot.flow.core.model.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.model.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.model.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.impl.service.instance.FlowRuntimeService;
import com.plasticene.boot.flow.core.impl.task.FlowTaskExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证任务初读拒绝及等待实例锁期间的状态变化处理。
 * @author ZFJ
 * @since 2026-11-10
 */
class FlowTaskExecutionTest {
    private final FlowTaskDAO tasks = mock(FlowTaskDAO.class);
    private final FlowRuntimeService runtime = mock(FlowRuntimeService.class);
    private final FlowTaskExecution execution = new FlowTaskExecution();
    private final LoginUser user = new LoginUser();
    private final FlowInstance instance = new FlowInstance();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(execution, "flowTaskDAO", tasks);
        ReflectionTestUtils.setField(execution, "flowRuntimeService", runtime);
        user.setId(8L);
        user.setOrgId(9L);
        instance.setId(21L);
        instance.setOrgId(9L);
        instance.setStatus(FlowInstanceStatusEnum.RUNNING.getCode());
    }

    @Test
    void shouldRejectUnauthorizedInitialReadWithoutAcquiringLock() {
        FlowTask task = runningTask();
        task.setAssignee(99L);
        when(tasks.selectById(11L)).thenReturn(task);
        assertThrows(BizException.class, () -> execution.execute(11L, user,
                (latest, locked) -> fail("无权操作时不能执行业务")));
        verifyNoInteractions(runtime);
    }

    @Test
    void shouldRejectAlreadyCompletedInitialReadWithoutAcquiringLock() {
        FlowTask task = runningTask();
        task.setStatus(FlowTaskStatusEnum.APPROVE.getCode());
        when(tasks.selectById(11L)).thenReturn(task);
        assertThrows(BizException.class, () -> execution.execute(11L, user,
                (latest, locked) -> fail("已完成任务不能执行业务")));
        verifyNoInteractions(runtime);
    }

    @Test
    void shouldRejectTaskCompletedWhileWaitingForLock() {
        FlowTask completed = runningTask();
        completed.setStatus(FlowTaskStatusEnum.APPROVE.getCode());
        when(tasks.selectById(11L)).thenReturn(runningTask(), completed);
        when(runtime.selectInstanceForUpdate(21L)).thenReturn(instance);
        AtomicBoolean executed = new AtomicBoolean();
        assertThrows(BizException.class, () -> execution.execute(11L, user,
                (latest, locked) -> executed.set(true)));
        assertFalse(executed.get());
        var order = inOrder(tasks, runtime);
        order.verify(tasks).selectById(11L);
        order.verify(runtime).selectInstanceForUpdate(21L);
        order.verify(tasks).selectById(11L);
    }

    @Test
    void shouldExecuteUsingTheLatestTaskUnderTheInstanceLock() {
        FlowTask latest = runningTask();
        latest.setComment("锁内最新数据");
        when(tasks.selectById(11L)).thenReturn(runningTask(), latest);
        when(runtime.selectInstanceForUpdate(21L)).thenReturn(instance);
        AtomicBoolean executed = new AtomicBoolean();
        execution.execute(11L, user, (task, locked) -> {
            assertSame(latest, task);
            assertSame(instance, locked);
            executed.set(true);
        });
        assertTrue(executed.get());
    }

    @Test
    void shouldRejectInstanceCancelledWhileWaitingForLock() {
        when(tasks.selectById(11L)).thenReturn(runningTask());
        instance.setStatus(FlowInstanceStatusEnum.CANCEL.getCode());
        when(runtime.selectInstanceForUpdate(21L)).thenReturn(instance);
        assertThrows(BizException.class, () -> execution.execute(11L, user,
                (latest, locked) -> fail("实例取消后不能执行业务")));
    }

    @Test
    void shouldReadRunningInstanceWithoutAcquiringALock() {
        when(runtime.getById(21L)).thenReturn(instance);
        assertSame(instance, execution.getRunningInstance(runningTask(), user));
        verify(runtime, never()).selectInstanceForUpdate(anyLong());
    }

    @Test
    void shouldRejectAnotherOrganizationInTheUnlockedQuery() {
        instance.setOrgId(99L);
        when(runtime.getById(21L)).thenReturn(instance);
        assertThrows(BizException.class, () -> execution.getRunningInstance(runningTask(), user));
        verify(runtime, never()).selectInstanceForUpdate(anyLong());
    }

    private FlowTask runningTask() {
        FlowTask task = new FlowTask();
        task.setId(11L);
        task.setInstanceId(21L);
        task.setOrgId(9L);
        task.setAssignee(8L);
        task.setNodeType(FlowNodeEnum.Type.APPROVE.getCode());
        task.setStatus(FlowTaskStatusEnum.RUNNING.getCode());
        task.setIsDelete(CommonConstant.IS_NOT_DEL);
        return task;
    }
}
