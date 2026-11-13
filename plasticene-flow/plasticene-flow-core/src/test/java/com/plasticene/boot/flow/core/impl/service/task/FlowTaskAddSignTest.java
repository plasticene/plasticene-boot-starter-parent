package com.plasticene.boot.flow.core.impl.service.task;

import com.plasticene.boot.flow.core.impl.service.instance.FlowRuntimeServiceImpl;
import com.plasticene.boot.flow.core.impl.service.timeout.FlowTimeoutServiceImpl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.utils.PtcBeanUtils;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowInstanceDAO;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowTaskAddSignDAO;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowTimeoutNoticeDAO;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowDefinition;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowInstance;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTask;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTaskAddSign;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTimeoutNotice;
import com.plasticene.boot.flow.core.model.enums.FlowAddSignEnum;
import com.plasticene.boot.flow.core.model.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.model.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.model.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.impl.executor.DefaultProcessExecutor;
import com.plasticene.boot.flow.core.impl.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.model.FlowNode;
import com.plasticene.boot.flow.core.model.FlowTimeoutRule;
import com.plasticene.boot.flow.core.api.param.FlowTaskAddSignParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskReturnParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskTransferParam;
import com.plasticene.boot.flow.core.api.spi.FlowOrganizationProvider;
import com.plasticene.boot.flow.core.impl.service.definition.FlowDefinitionService;
import com.plasticene.boot.flow.core.impl.service.instance.FlowRuntimeService;
import java.util.ArrayList;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.test.util.ReflectionTestUtils;

import com.plasticene.boot.flow.core.api.event.FlowEvent;
import com.plasticene.boot.flow.core.impl.event.FlowEventPublisher;
import com.plasticene.boot.flow.core.api.event.FlowEventType;
import com.plasticene.boot.flow.core.api.event.FlowTaskAction;
import com.plasticene.boot.flow.core.api.event.FlowTaskCancelReason;
import com.plasticene.boot.flow.core.api.event.FlowTaskEvent;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 通过执行真实服务生成的查询与更新条件验证加签组结算及原节点衔接。
 * @author ZFJ
 * @since 2026-11-03
 */
class FlowTaskAddSignTest {
    private LoginUser loginUserContext;
    private final List<FlowTaskEvent> events = new ArrayList<>();
    private final List<FlowEvent> lifecycleEvents = new ArrayList<>();
    private final FlowTaskServiceImpl service = spy(new FlowTaskServiceImpl());
    private final FlowTaskDAO tasks = mock(FlowTaskDAO.class);
    private final FlowTaskAddSignDAO signs = mock(FlowTaskAddSignDAO.class);
    private final FlowTimeoutNoticeDAO notices = mock(FlowTimeoutNoticeDAO.class);
    private final FlowRuntimeService runtime = mock(FlowRuntimeService.class);
    private final FlowDefinitionService definitions = mock(FlowDefinitionService.class);
    private final FlowOrganizationProvider organization = mock(FlowOrganizationProvider.class);
    private final ProcessExecutor executor = mock(ProcessExecutor.class);
    private final Map<Long, FlowTask> rows = new LinkedHashMap<>();
    private final Map<Long, FlowTaskAddSign> batches = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong(100);
    private final FlowInstance instance = new FlowInstance();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        FlowTaskTestComponents.install(service);
        FlowEventPublisher publisher = event -> {
            lifecycleEvents.add(event);
            if (event instanceof FlowTaskEvent taskEvent) {
                events.add(taskEvent);
            }
        };
        FlowTaskTestComponents.setDependency(service, "flowEventPublisher", publisher);
        FlowRuntimeServiceImpl terminalService = new FlowRuntimeServiceImpl();
        ReflectionTestUtils.setField(terminalService, "flowTaskService", service);
        ReflectionTestUtils.setField(terminalService, "flowInstanceDAO",
                mock(FlowInstanceDAO.class));
        ReflectionTestUtils.setField(terminalService, "flowEventPublisher", publisher);
        doAnswer(call -> {
            terminalService.endInstance(call.getArgument(0), call.getArgument(1), call.getArgument(2));
            return null;
        }).when(runtime).endInstance(any(), any(), any());
        when(runtime.getById(anyLong())).thenReturn(instance);
        for (Class<?> type : List.of(FlowTask.class, FlowTaskAddSign.class, FlowTimeoutNotice.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "add-sign"), type);
        }
        FlowTaskTestComponents.setDependency(service, "flowTaskDAO", tasks);
        FlowTaskTestComponents.setDependency(service, "flowTaskAddSignDAO", signs);
        FlowTaskTestComponents.setDependency(service, "flowTimeoutNoticeDAO", notices);
        FlowTaskTestComponents.setDependency(service, "flowRuntimeService", runtime);
        FlowTaskTestComponents.setDependency(service, "flowDefinitionService", definitions);
        FlowTaskTestComponents.setDependency(service, "flowOrganizationProvider", organization);
        FlowTaskTestComponents.setDependency(service, "processExecutor", executor);
        instance.setId(21L);
        instance.setOrgId(9L);
        instance.setDefinitionId(31L);
        instance.setStatus(0);
        when(runtime.selectInstanceForUpdate(21L)).thenReturn(instance);
        when(organization.getActiveUserMap(ArgumentMatchers.anyLong())).thenReturn(Map.of(8L, "原审批人", 9L, "原后续人", 10L, "加签甲", 11L, "加签乙", 12L, "接收人"));
        FlowDefinition definition = new FlowDefinition();
        definition.setOrgId(9L);
        FlowNode node = new FlowNode();
        node.setKey("approve");
        node.setType(1);
        node.setApproveType(0);
        definition.setModelNode(node);
        when(definitions.getById(31L)).thenReturn(definition);
        when(tasks.selectById(anyLong())).thenAnswer(call -> copy(rows.get(call.<Long>getArgument(0))));
        when(tasks.selectList(any(Wrapper.class))).thenAnswer(call -> select(rows, call.getArgument(0)));
        when(tasks.insert(any(FlowTask.class))).thenAnswer(call -> {
            FlowTask task = call.getArgument(0);
            task.setId(sequence.incrementAndGet());
            rows.put(task.getId(), copy(task));
            return 1;
        });
        when(tasks.update(any(FlowTask.class), any(Wrapper.class))).thenAnswer(call -> patch(rows, call.getArgument(1), call.getArgument(0)));
        when(tasks.update(any(Wrapper.class))).thenAnswer(call -> update(rows, call.getArgument(0)));
        when(signs.selectById(anyLong())).thenAnswer(call -> batches.get(call.<Long>getArgument(0)));
        when(signs.selectList(any(Wrapper.class))).thenAnswer(call -> select(batches, call.getArgument(0)));
        when(signs.insert(any(FlowTaskAddSign.class))).thenAnswer(call -> {
            FlowTaskAddSign batch = call.getArgument(0);
            batch.setId(sequence.incrementAndGet());
            batches.put(batch.getId(), batch);
            return 1;
        });
        when(signs.updateById(any(FlowTaskAddSign.class))).thenReturn(1);
        rows.put(1L, task(1L, 8L, "approve", 0));
        login(8L, 9L);
    }

    @Test
    void shouldPublishTransferCompletionBeforeNewTodoWithStableSnapshots() {
        FlowTaskTransferParam param = new FlowTaskTransferParam();
        param.setTaskId(1L);
        param.setAssignee(12L);
        param.setComment("转交审核");
        service.transferTask(param, loginUserContext);
        assertEquals(List.of(FlowEventType.TASK_COMPLETED, FlowEventType.TASK_CREATED),
                events.stream().map(FlowTaskEvent::eventType).toList());
        assertEquals(FlowTaskAction.TRANSFER, events.getFirst().action());
        assertEquals(1L, events.getFirst().taskId());
        assertEquals(8L, events.getFirst().assignee());
        assertEquals(12L, events.getLast().assignee());
        assertNotEquals(events.getFirst().taskId(), events.getLast().taskId());
    }

    @Test
    void shouldClosePausedTodoAndReopenItOnlyAfterBeforeSignCompletes() {
        service.addSign(request(1L, FlowAddSignEnum.Type.BEFORE, 0, List.of(10L)), loginUserContext);
        assertEquals(FlowEventType.TASK_CANCELLED, events.getFirst().eventType());
        assertEquals(FlowTaskCancelReason.ADD_SIGN_WAITING, events.getFirst().cancelReason());
        assertEquals(FlowEventType.TASK_CREATED, events.getLast().eventType());
        assertEquals(10L, events.getLast().assignee());
        approve(pending(10L));
        assertEquals(FlowEventType.TASK_CREATED, events.getLast().eventType());
        assertEquals(1L, events.getLast().taskId());
        assertEquals(8L, events.getLast().assignee());
    }

    @Test
    void shouldQueryOnlyCurrentNodeTodosWhenAnyApproverCompletes() {
        rows.get(1L).setApproveMode(1);
        rows.put(2L, task(2L, 9L, "approve", 1));
        rows.put(3L, task(3L, 12L, "other-branch", 0));
        approve(rows.get(1L));

        var captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(tasks).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("node_key ="));
        assertTrue(sql.contains("instance_id ="));
        assertTrue(sql.contains("node_type ="));
        assertTrue(sql.contains("status ="));
        assertTrue(sql.contains("is_delete ="));
        assertTrue(sql.contains("return_task_id IS NULL"));
        assertEquals(1, rows.get(2L).getIsDelete());
        assertEquals(0, rows.get(3L).getIsDelete());
        assertEquals(0, rows.get(3L).getStatus());
        assertEquals(List.of(1L, 2L), events.stream().map(FlowTaskEvent::taskId).toList());
        assertEquals(FlowTaskCancelReason.NODE_COMPLETED, events.getLast().cancelReason());
    }

    @Test
    void shouldCancelOnlyRemainingAnySignTodosOnce() {
        rows.get(1L).setApproveMode(1);
        rows.put(2L, task(2L, 9L, "approve", 1));
        rows.put(3L, task(3L, 12L, "other-branch", 0));
        approve(rows.get(1L));
        assertEquals(List.of(FlowEventType.TASK_COMPLETED, FlowEventType.TASK_CANCELLED),
                events.stream().map(FlowTaskEvent::eventType).toList());
        assertEquals(2L, events.getLast().taskId());
        assertEquals(FlowTaskCancelReason.NODE_COMPLETED, events.getLast().cancelReason());
        assertThrows(BizException.class, () -> approve(rows.get(1L)));
        assertEquals(2, events.size());
    }

    @Test
    void shouldCancelAllActionableTodosWhenInstanceCancelled() {
        service.addSign(request(1L, FlowAddSignEnum.Type.BEFORE, 0, List.of(10L, 11L)), loginUserContext);
        events.clear();
        service.cancelInstanceRunningTask(21L);
        assertEquals(2, events.size());
        assertTrue(events.stream().allMatch(event -> event.eventType() == FlowEventType.TASK_CANCELLED
                && event.cancelReason() == FlowTaskCancelReason.INSTANCE_CANCELLED));
        assertEquals(2, events.stream().map(FlowTaskEvent::taskId).distinct().count());
    }

    @ParameterizedTest
    @CsvSource({"BEFORE,0", "BEFORE,1", "BEFORE,2", "AFTER,0", "AFTER,1", "AFTER,2"})
    void shouldSettleGroupBeforeResumingOrCompletingOriginalTask(FlowAddSignEnum.Type type, int mode) {
        LocalDateTime deadline = rows.get(1L).getDeadline();
        LocalDateTime remindAt = rows.get(1L).getNextRemindTime();
        service.addSign(request(1L, type, mode, List.of(10L, 11L)), loginUserContext);
        FlowTaskAddSign batch = batches.values().iterator().next();
        batch.setPausedAt(LocalDateTime.now().minusMinutes(10));
        assertEquals(6, rows.get(1L).getStatus());
        assertEquals(deadline, rows.get(1L).getDeadline());
        assertEquals(mode == 2 ? 1 : 2, runningMembers(batch.getId()).size());
        FlowTask first = runningMembers(batch.getId()).getFirst();
        assertTrue(first.getDeadline().isAfter(first.getStartTime()));
        approve(first);
        if (mode != 1) {
            assertEquals(6, rows.get(1L).getStatus());
            verifyNoInteractions(executor);
            approve(runningMembers(batch.getId()).getFirst());
        }
        assertEquals(FlowAddSignEnum.State.PASSED, batch.getState());
        assertTrue(runningMembers(batch.getId()).isEmpty());
        if (type == FlowAddSignEnum.Type.BEFORE) {
            assertEquals(0, rows.get(1L).getStatus());
            assertTrue(rows.get(1L).getDeadline().isAfter(deadline.plusMinutes(9)));
            assertTrue(rows.get(1L).getNextRemindTime().isAfter(remindAt.plusMinutes(9)));
            assertEquals(2, rows.get(1L).getRemindCount());
            verifyNoInteractions(executor);
            approve(rows.get(1L));
        } else {
            assertEquals(1, rows.get(1L).getStatus());
            assertEquals("补充审核", rows.get(1L).getComment());
            assertEquals(8L, rows.get(1L).getAssignee());
        }
        verify(executor, times(1)).moveToNextNode(any(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void shouldAdvanceAfterBeforeAddSignWithReturnedHistory(int mode) {
        FlowTask returned = task(2L, 8L, "approve", 0);
        returned.setStatus(FlowTaskStatusEnum.RETURNED.getCode());
        rows.put(returned.getId(), returned);
        service.addSign(request(1L, FlowAddSignEnum.Type.BEFORE, mode, List.of(10L, 11L)), loginUserContext);
        FlowTaskAddSign batch = batches.values().iterator().next();
        approve(runningMembers(batch.getId()).getFirst());
        if (mode != 1) {
            approve(runningMembers(batch.getId()).getFirst());
        }
        assertEquals(FlowAddSignEnum.State.PASSED, batch.getState());
        verifyNoInteractions(executor);
        approve(rows.get(1L));
        assertEquals(FlowTaskStatusEnum.APPROVE.getCode(), rows.get(1L).getStatus());
        assertEquals(FlowTaskStatusEnum.RETURNED.getCode(), rows.get(2L).getStatus());
        verify(executor, times(1)).moveToNextNode(eq(instance), any());
    }

    @Test
    void shouldWaitForOtherOriginalApproverAfterBeforeAddSign() {
        rows.put(2L, task(2L, 9L, "approve", 0));
        service.addSign(request(1L, FlowAddSignEnum.Type.BEFORE, 0, List.of(10L)), loginUserContext);
        approve(pending(10L));
        approve(rows.get(1L));
        verifyNoInteractions(executor);
        approve(rows.get(2L));
        verify(executor, times(1)).moveToNextNode(eq(instance), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void shouldApplyParallelSignUsingOriginalNodeMode(int mode) {
        rows.get(1L).setApproveMode(mode);
        if (mode == 2) rows.get(1L).setAssigneeList(List.of(8L, 9L));
        service.addSign(request(1L, FlowAddSignEnum.Type.PARALLEL, 99, List.of(10L, 11L)), loginUserContext);
        FlowTaskAddSign batch = batches.values().iterator().next();
        assertEquals(mode, batch.getApproveMode());
        assertEquals(FlowAddSignEnum.State.APPLIED, batch.getState());
        assertEquals(0, rows.get(1L).getStatus());
        if (mode == 2) {
            assertEquals(List.of(8L, 10L, 11L, 9L), rows.get(1L).getAssigneeList());
            assertEquals(1, rows.size());
            approve(rows.get(1L));
            FlowTask first = pending(10L);
            assertEquals(batch.getId(), first.getAddSignId());
            approve(first);
            approve(pending(11L));
            assertNull(pending(9L).getAddSignId());
            approve(pending(9L));
        } else if (mode == 0) {
            approve(pending(10L));
            approve(pending(11L));
            verifyNoInteractions(executor);
            approve(rows.get(1L));
        } else {
            approve(pending(10L));
            assertEquals(1, rows.get(1L).getIsDelete());
            assertEquals(1, rows.values().stream().filter(t -> Objects.equals(t.getAssignee(), 11L)).findFirst().orElseThrow().getIsDelete());
        }
        verify(executor, times(1)).moveToNextNode(any(), any());
    }

    @Test
    void shouldCancelSignGroupWhenAnotherOriginalAnyApproverWinsWithoutTouchingSiblingBranch() {
        rows.get(1L).setApproveMode(1);
        rows.put(2L, task(2L, 9L, "approve", 1));
        rows.put(3L, task(3L, 12L, "other-branch", 0));
        service.addSign(request(1L, FlowAddSignEnum.Type.BEFORE, 0, List.of(10L, 11L)), loginUserContext);
        FlowTask member = pending(10L);
        approve(rows.get(2L));
        assertEquals(3, rows.get(1L).getStatus());
        assertEquals(FlowAddSignEnum.State.CANCELLED, batches.values().iterator().next().getState());
        assertTrue(rows.values().stream().filter(t -> t.getAddSignId() != null).allMatch(t -> t.getStatus() == 3));
        assertEquals(0, rows.get(3L).getStatus());
        assertEquals(0, rows.get(3L).getIsDelete());
        assertThrows(BizException.class, () -> approve(member));
    }

    @Test
    void shouldRejectWholeInstanceWhenSignMemberRejects() {
        service.addSign(request(1L, FlowAddSignEnum.Type.AFTER, 0, List.of(10L, 11L)), loginUserContext);
        FlowTask member = pending(10L);
        login(10L, 9L);
        FlowTaskParam param = action(member.getId());
        service.rejectTask(param, loginUserContext);
        assertEquals(2, rows.get(member.getId()).getStatus());
        assertEquals(3, rows.get(1L).getStatus());
        assertEquals(FlowAddSignEnum.State.REJECTED, batches.values().iterator().next().getState());
        verify(runtime).endInstance(any(), any(), eq(FlowInstanceStatusEnum.REJECT));
        verify(service).delInstanceRunningTask(21L);
        assertEquals(0, rows.get(member.getId()).getIsDelete());
        assertEquals(2, instance.getStatus());
        assertEquals(FlowEventType.INSTANCE_REJECTED, lifecycleEvents.getLast().eventType());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldCleanParallelTasksAndAddSignsOnceForAutomaticOrManualRejection(boolean automatic) {
        service.addSign(request(1L, FlowAddSignEnum.Type.BEFORE, 0, List.of(10L, 11L)), loginUserContext);
        rows.put(2L, task(2L, 8L, "other-branch", 0));
        FlowNode otherBranch = new FlowNode();
        otherBranch.setKey("other-branch");
        otherBranch.setType(FlowNodeEnum.Type.APPROVE.getCode());
        definitions.getById(31L).getModelNode().setChildNode(otherBranch);
        events.clear();
        lifecycleEvents.clear();
        if (automatic) {
            var process = new DefaultProcessExecutor();
            ReflectionTestUtils.setField(process, "flowTaskService", service);
            ReflectionTestUtils.setField(process, "flowRuntimeService", runtime);
            FlowNode node = new FlowNode();
            node.setKey("auto-reject");
            node.setType(FlowNodeEnum.Type.APPROVE.getCode());
            node.setApproveType(FlowNodeEnum.ApproveType.AUTO_REJECT.getCode());
            process.executeNode(instance, node);
        } else {
            service.rejectTask(action(2L), loginUserContext);
        }
        verify(service).delInstanceRunningTask(21L);
        verify(runtime).endInstance(eq(instance), any(), eq(FlowInstanceStatusEnum.REJECT));
        assertEquals(FlowAddSignEnum.State.CANCELLED, batches.values().iterator().next().getState());
        assertEquals(3, rows.get(1L).getStatus());
        assertTrue(rows.values().stream().filter(t -> t.getAddSignId() != null).allMatch(t -> t.getStatus() == 3));
        FlowTask rejected = rows.values().stream().filter(t -> t.getStatus() == 2).findFirst().orElseThrow();
        assertNotEquals(1, rejected.getIsDelete());
        assertEquals(automatic ? 1 : 0, rows.get(2L).getIsDelete());
        assertEquals(FlowEventType.TASK_COMPLETED, lifecycleEvents.getFirst().eventType());
        assertEquals(FlowTaskAction.REJECT, ((FlowTaskEvent) lifecycleEvents.getFirst()).action());
        assertTrue(lifecycleEvents.subList(1, lifecycleEvents.size() - 1).stream()
                .allMatch(event -> event.eventType() == FlowEventType.TASK_CANCELLED));
        assertTrue(lifecycleEvents.size() >= 3);
        assertEquals(FlowEventType.INSTANCE_REJECTED, lifecycleEvents.getLast().eventType());
        assertEquals(2, instance.getStatus());
    }

    @Test
    void shouldRestrictWaitingAndGroupTasksAndRejectInvalidPeople() {
        assertThrows(BizException.class, () -> service.addSign(request(1L, FlowAddSignEnum.Type.BEFORE, 0, List.of(8L)), loginUserContext));
        assertThrows(BizException.class, () -> service.addSign(request(1L, FlowAddSignEnum.Type.BEFORE, 0, List.of(10L, 10L)), loginUserContext));
        assertThrows(BizException.class, () -> service.addSign(request(1L, FlowAddSignEnum.Type.BEFORE, 0, List.of(99L)), loginUserContext));
        assertThrows(BizException.class, () -> service.addSign(request(1L, FlowAddSignEnum.Type.BEFORE, 9, List.of(10L)), loginUserContext));
        login(8L, 99L);
        assertThrows(BizException.class, () -> service.addSign(request(1L, FlowAddSignEnum.Type.BEFORE, 0, List.of(10L)), loginUserContext));
        login(8L, 9L);
        service.addSign(request(1L, FlowAddSignEnum.Type.BEFORE, 0, List.of(10L, 11L)), loginUserContext);
        assertThrows(BizException.class, () -> service.approveTask(action(1L), loginUserContext));
        assertThrows(BizException.class, () -> service.addSign(request(1L, FlowAddSignEnum.Type.AFTER, 0, List.of(12L)), loginUserContext));
        FlowTask member = pending(10L);
        login(10L, 9L);
        assertThrows(BizException.class, () -> service.addSign(request(member.getId(), FlowAddSignEnum.Type.BEFORE, 0, List.of(12L)), loginUserContext));
        FlowTaskTransferParam transfer = new FlowTaskTransferParam();
        transfer.setTaskId(member.getId()); transfer.setAssignee(12L); transfer.setComment("转交");
        assertThrows(BizException.class, () -> service.transferTask(transfer, loginUserContext));
        assertThrows(BizException.class, () -> service.listReturnTargets(member.getId(), loginUserContext));
        FlowTaskReturnParam back = new FlowTaskReturnParam();
        back.setTaskId(member.getId()); back.setTargetNodeKey("previous"); back.setComment("退回");
        assertThrows(BizException.class, () -> service.returnTask(back, loginUserContext));
    }

    @Test
    void shouldCancelWaitingParentsAndAllGroupsWhenInstanceIsCancelled() {
        service.addSign(request(1L, FlowAddSignEnum.Type.BEFORE, 2, List.of(10L, 11L)), loginUserContext);
        service.cancelInstanceRunningTask(21L);
        assertEquals(3, rows.get(1L).getStatus());
        assertEquals(FlowAddSignEnum.State.CANCELLED, batches.values().iterator().next().getState());
        assertTrue(rows.values().stream().noneMatch(t -> t.getStatus() == 0 || t.getStatus() == 6));
    }

    @Test
    void shouldRejectPreviouslyHandledAndQueuedPeopleAndRepeatedSubmission() {
        rows.get(1L).setApproveMode(2);
        rows.get(1L).setAssigneeList(List.of(8L, 9L));
        assertThrows(BizException.class, () -> service.addSign(request(1L, FlowAddSignEnum.Type.PARALLEL, null, List.of(9L)), loginUserContext));
        service.addSign(request(1L, FlowAddSignEnum.Type.PARALLEL, null, List.of(10L, 11L)), loginUserContext);
        assertThrows(BizException.class, () -> service.addSign(request(1L, FlowAddSignEnum.Type.PARALLEL, null, List.of(10L)), loginUserContext));
        approve(rows.get(1L));
        login(8L, 9L);
        assertThrows(BizException.class, () -> service.addSign(request(1L, FlowAddSignEnum.Type.PARALLEL, null, List.of(12L)), loginUserContext));
    }

    @Test
    void shouldPauseOriginalTimeoutAndSettleSignMemberTimeoutThroughGroupLogic() {
        FlowTaskTestComponents.setDependency(service, "baseMapper", tasks);
        rows.get(1L).getTimeoutRule().setAction(FlowTimeoutRule.Action.APPROVE);
        rows.get(1L).setDeadline(LocalDateTime.now().minusMinutes(1));
        service.addSign(request(1L, FlowAddSignEnum.Type.BEFORE, 1, List.of(10L, 11L)), loginUserContext);
        FlowTimeoutServiceImpl timeouts = new FlowTimeoutServiceImpl(notices, runtime, service);
        timeouts.handleDueTask(rows.get(1L), LocalDateTime.now());
        assertEquals(6, rows.get(1L).getStatus());
        FlowTask member = pending(10L);
        when(tasks.update(isNull(), any(Wrapper.class))).thenAnswer(call -> update(rows, call.getArgument(1)));
        timeouts.handleDueTask(member, member.getDeadline().plusSeconds(1));
        assertEquals(1, rows.get(member.getId()).getStatus());
        assertEquals(Boolean.TRUE, rows.get(member.getId()).getTimeoutHandled());
        assertEquals(0, rows.get(1L).getStatus());
        assertEquals(FlowAddSignEnum.State.PASSED, batches.values().iterator().next().getState());
        verifyNoInteractions(executor);
        // 原持有人仍须审批，系统加签通过不能直接推进原节点。
    }

    @Test
    void shouldRecheckTaskAfterAcquiringInstanceLock() {
        when(runtime.selectInstanceForUpdate(21L)).thenAnswer(call -> {
            rows.get(1L).setStatus(1);
            return instance;
        });
        assertThrows(BizException.class, () -> service.addSign(request(1L, FlowAddSignEnum.Type.BEFORE, 0, List.of(10L)), loginUserContext));
        assertTrue(batches.isEmpty());
        verify(tasks, never()).insert(any(FlowTask.class));
    }

    @Test
    void shouldRejectRepeatedGroupApprovalAndDenyWithdrawalAcrossSignGroup() {
        service.addSign(request(1L, FlowAddSignEnum.Type.BEFORE, 1, List.of(10L, 11L)), loginUserContext);
        FlowTask member = pending(10L);
        approve(member);
        assertThrows(BizException.class, () -> approve(member));
        verifyNoInteractions(executor);
        approve(rows.get(1L));
        FlowDefinition definition = new FlowDefinition();
        assertFalse(service.canWithdrawTask(rows.get(1L), instance, definition, List.copyOf(rows.values()), loginUserContext));
        login(10L, 9L);
        assertFalse(service.canWithdrawTask(rows.get(member.getId()), instance, definition, List.copyOf(rows.values()), loginUserContext));
    }

    @Test
    void shouldNotWithdrawEarlierApprovalAfterLaterTaskHasStartedSignGroup() {
        FlowTask previous = task(0L, 8L, "approve", 2);
        previous.setStatus(1);
        previous.setEndTime(LocalDateTime.now().minusMinutes(1));
        rows.put(0L, previous);
        rows.get(1L).setApproveMode(2);
        rows.get(1L).setAssigneeList(List.of(8L, 9L));
        service.addSign(request(1L, FlowAddSignEnum.Type.BEFORE, 0, List.of(10L)), loginUserContext);
        FlowDefinition definition = new FlowDefinition();
        assertFalse(service.canWithdrawTask(previous, instance, definition, List.copyOf(rows.values()), loginUserContext));
        assertEquals(6, rows.get(1L).getStatus());
        assertEquals(1, runningMembers(batches.values().iterator().next().getId()).size());
    }

    @Test
    void shouldInvalidateWaitingParentAndGroupWhenAnotherNodeTaskReturnsToPreviousNode() {
        FlowNode previous = new FlowNode();
        previous.setKey("previous"); previous.setName("前序审批"); previous.setType(1); previous.setApproveType(0);
        FlowNode current = new FlowNode();
        current.setKey("approve"); current.setType(1); current.setApproveType(0);
        previous.setChildNode(current);
        FlowDefinition definition = new FlowDefinition();
        definition.setOrgId(9L); definition.setModelNode(previous);
        when(definitions.getById(31L)).thenReturn(definition);
        FlowTask prior = task(0L, 8L, "previous", 0);
        prior.setStatus(1); prior.setEndTime(LocalDateTime.now().minusMinutes(1));
        rows.put(0L, prior);
        rows.put(2L, task(2L, 9L, "approve", 0));
        rows.put(3L, task(3L, 12L, "unrelated", 0));
        service.addSign(request(1L, FlowAddSignEnum.Type.BEFORE, 2, List.of(10L, 11L)), loginUserContext);
        login(9L, 9L);
        FlowTaskReturnParam param = new FlowTaskReturnParam();
        param.setTaskId(2L); param.setTargetNodeKey("previous"); param.setComment("补充材料");
        service.returnTask(param, loginUserContext);
        assertEquals(3, rows.get(1L).getStatus());
        assertEquals(2L, rows.get(1L).getReturnTaskId());
        assertEquals(FlowAddSignEnum.State.CANCELLED, batches.values().iterator().next().getState());
        assertTrue(rows.values().stream().filter(task -> task.getAddSignId() != null)
                .allMatch(task -> task.getStatus() == 3 && Objects.equals(task.getReturnTaskId(), 2L)));
        assertEquals(0, rows.get(3L).getStatus());
        assertNull(rows.get(3L).getReturnTaskId());
        verify(executor).executeNode(instance, previous);
    }

    @ParameterizedTest
    @EnumSource(FlowAddSignEnum.Type.class)
    void shouldRejectEveryAddSignTypeWhenPublishedNodeDisablesIt(FlowAddSignEnum.Type type) {
        definitions.getById(31L).getModelNode().setAllowAddSign(false);

        BizException error = assertThrows(BizException.class,
                () -> service.addSign(request(1L, type, 0, List.of(10L)), loginUserContext));

        assertEquals("当前审批节点不允许加签", error.getMessage());
        assertTrue(batches.isEmpty());
        assertEquals(0, rows.get(1L).getStatus());
        verify(signs, never()).insert(any(FlowTaskAddSign.class));
    }

    private FlowTaskAddSignParam request(Long taskId, FlowAddSignEnum.Type type, Integer mode, List<Long> ids) {
        FlowTaskAddSignParam param = new FlowTaskAddSignParam();
        param.setTaskId(taskId); param.setType(type); param.setApproveMode(mode); param.setAssigneeIds(ids); param.setComment("  补充审核  ");
        return param;
    }

    private FlowTask task(Long id, Long assignee, String key, int mode) {
        FlowTask task = new FlowTask();
        task.setId(id); task.setOrgId(9L); task.setInstanceId(21L); task.setNodeKey(key); task.setNodeName(key);
        task.setNodeType(1); task.setApproveType(0); task.setApproveMode(mode); task.setAssignee(assignee);
        task.setStatus(0); task.setIsDelete(0); task.setStartTime(LocalDateTime.now().minusMinutes(2));
        task.setDeadline(LocalDateTime.now().plusMinutes(5)); task.setNextRemindTime(LocalDateTime.now().plusMinutes(1)); task.setRemindCount(2);
        FlowTimeoutRule rule = new FlowTimeoutRule(); rule.setAutoEnabled(true); rule.setAutoAfter(10); rule.setAutoUnit(FlowTimeoutRule.Unit.MINUTE);
        task.setTimeoutRule(rule);
        return task;
    }

    private FlowTask pending(Long assignee) {
        return rows.values().stream().filter(t -> Objects.equals(t.getAssignee(), assignee) && t.getStatus() == 0 && t.getIsDelete() == 0).findFirst().orElseThrow();
    }

    private List<FlowTask> runningMembers(Long batchId) {
        return rows.values().stream().filter(t -> Objects.equals(t.getAddSignId(), batchId) && t.getStatus() == 0).toList();
    }

    private void approve(FlowTask task) {
        login(task.getAssignee(), task.getOrgId());
        service.approveTask(action(task.getId()), loginUserContext);
    }

    private FlowTaskParam action(Long id) {
        FlowTaskParam param = new FlowTaskParam(); param.setTaskId(id); param.setComment("同意"); return param;
    }

    private void login(Long id, Long org) {
        LoginUser user = new LoginUser(); user.setId(id); user.setOrgId(org); loginUserContext = user;
    }

    private FlowTask copy(FlowTask row) { return row == null ? null : PtcBeanUtils.copy(row, FlowTask.class); }

    private <T> List<T> select(Map<Long, T> table, Wrapper<T> wrapper) {
        String sql = wrapper.getSqlSegment().split("ORDER BY")[0];
        Pattern pattern = Pattern.compile("(\\w+) (IS NULL|IN \\([^)]+\\)|= #\\{[^}]+})");
        return table.values().stream().filter(row -> {
            var matcher = pattern.matcher(sql);
            while (matcher.find()) {
                Object actual = new BeanWrapperImpl(row).getPropertyValue(property(matcher.group(1)));
                String condition = matcher.group(2);
                if (condition.equals("IS NULL")) { if (actual != null) return false; }
                else if (condition.startsWith("IN")) {
                    var placeholders = Pattern.compile("#\\{[^}]+}").matcher(condition); boolean found = false;
                    while (placeholders.find()) found |= Objects.equals(actual, value(wrapper, placeholders.group()));
                    if (!found) return false;
                } else if (!Objects.equals(actual, value(wrapper, condition.substring(2)))) return false;
            }
            return true;
        }).toList();
    }

    private <T> int patch(Map<Long, T> table, Wrapper<T> wrapper, T patch) {
        List<T> selected = select(table, wrapper);
        BeanWrapperImpl source = new BeanWrapperImpl(patch);
        for (T row : selected) {
            BeanWrapperImpl target = new BeanWrapperImpl(row);
            for (var property : source.getPropertyDescriptors()) {
                String name = property.getName();
                if (source.isReadableProperty(name) && target.isWritableProperty(name) && source.getPropertyValue(name) != null) {
                    target.setPropertyValue(name, source.getPropertyValue(name));
                }
            }
        }
        return selected.size();
    }

    private <T> int update(Map<Long, T> table, LambdaUpdateWrapper<T> wrapper) {
        List<T> selected = select(table, wrapper);
        for (T row : selected) {
            var matcher = Pattern.compile("(\\w+)=(#\\{[^}]+}|null)").matcher(wrapper.getSqlSet());
            while (matcher.find()) {
                new BeanWrapperImpl(row).setPropertyValue(property(matcher.group(1)), value(wrapper, matcher.group(2)));
            }
        }
        return selected.size();
    }

    private Object value(Wrapper<?> wrapper, String placeholder) {
        if (placeholder.equals("null")) return null;
        String name = placeholder.substring(placeholder.lastIndexOf('.') + 1, placeholder.length() - 1);
        return ((AbstractWrapper<?, ?, ?>) wrapper).getParamNameValuePairs().get(name);
    }

    private String property(String column) {
        StringBuilder name = new StringBuilder(); boolean upper = false;
        for (char c : column.toCharArray()) { if (c == '_') upper = true; else { name.append(upper ? Character.toUpperCase(c) : c); upper = false; } }
        return name.toString();
    }
}
