package com.plasticene.boot.flow.core.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.flow.core.dao.FlowTimeoutNoticeDAO;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.entity.FlowTimeoutNotice;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowTimeoutNoticeEnums.EventType;
import com.plasticene.boot.flow.core.enums.FlowTimeoutNoticeEnums.State;
import com.plasticene.boot.flow.core.model.dto.FlowTimeoutRule;
import com.plasticene.boot.flow.core.provider.FlowTimeoutNotification;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 验证超时通知类型、状态流转及有限重试的兼容行为。
 * @author ZFJ
 * @since 2026-10-30
 */
@ExtendWith(MockitoExtension.class)
class FlowTimeoutServiceImplTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 30, 10, 0);

    @Mock
    private FlowTimeoutNoticeDAO dao;
    @Mock
    private FlowRuntimeService runtime;
    @Mock
    private FlowTaskService tasks;
    @Mock
    private FlowTimeoutNotification channel;
    private FlowTimeoutServiceImpl service;
    private FlowTask task;
    private FlowInstance instance;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), FlowTask.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), FlowTimeoutNotice.class);
        service = new FlowTimeoutServiceImpl(dao, runtime, tasks);
        task = new FlowTask();
        task.setId(11L);
        task.setOrgId(9L);
        task.setInstanceId(21L);
        task.setAssignee(8L);
        task.setStatus(FlowTaskStatusEnum.RUNNING.getCode());
        task.setIsDelete(CommonConstant.IS_NOT_DEL);
        task.setNodeType(FlowNodeEnum.Type.APPROVE.getCode());
        FlowTimeoutRule rule = new FlowTimeoutRule();
        rule.setMaxReminders(2);
        rule.setReminderInterval(10);
        rule.setReminderIntervalUnit(FlowTimeoutRule.Unit.MINUTE);
        rule.setAction(FlowTimeoutRule.Action.APPROVE);
        task.setTimeoutRule(rule);
        instance = new FlowInstance();
        instance.setId(21L);
        instance.setOrgId(9L);
        instance.setUserId(7L);
        instance.setStatus(FlowInstanceStatusEnum.RUNNING.getCode());
    }

    @Test
    void shouldCreateFirstReminderWithCompatibleCodes() {
        task.setNextRemindTime(NOW);
        prepareDueTask();

        service.handleDueTask(task, NOW);

        FlowTimeoutNotice notice = insertedNotice();
        assertEquals("REMINDER", notice.getEventType());
        assertEquals("PENDING", notice.getState());
        assertEquals(1, notice.getRoundNumber());
        assertEquals(0, notice.getAttempts());
        assertEquals(NOW, notice.getRetryAt());
        verify(service, never()).doExecuteTimeoutTask(any());
    }

    @Test
    void shouldCreateResultWithRoundZeroAfterAutomaticHandling() {
        task.setDeadline(NOW);
        prepareDueTask();

        service.handleDueTask(task, NOW);

        FlowTimeoutNotice notice = insertedNotice();
        assertEquals("RESULT", notice.getEventType());
        assertEquals("PENDING", notice.getState());
        assertEquals(0, notice.getRoundNumber());
        verify(service).doExecuteTimeoutTask(task);
    }

    @Test
    void shouldSendReminderOnlyToAssignee() {
        FlowTimeoutNotice notice = prepareNotice(EventType.REMINDER, State.PENDING, 0);
        when(runtime.getById(21L)).thenReturn(instance);
        notice.setError("previous failure");

        service.sendNotification(31L, NOW);

        ArgumentCaptor<FlowTimeoutNotification.Message> message = ArgumentCaptor.forClass(FlowTimeoutNotification.Message.class);
        verify(channel).send(message.capture());
        assertEquals("REMINDER", message.getValue().eventType());
        assertEquals(List.of(8L), message.getValue().recipients());
        assertEquals("SENT", notice.getState());
        assertEquals(1, notice.getAttempts());
        assertEquals(NOW, notice.getSentTime());
        assertNull(notice.getError());
        verify(dao).update(any());
    }

    @Test
    void shouldSendResultToAssigneeAndApplicantEvenWhenTaskFinished() {
        task.setStatus(FlowTaskStatusEnum.APPROVE.getCode());
        FlowTimeoutNotice notice = prepareNotice(EventType.RESULT, State.PENDING, 0);
        when(runtime.getById(21L)).thenReturn(instance);

        service.sendNotification(31L, NOW);

        ArgumentCaptor<FlowTimeoutNotification.Message> message = ArgumentCaptor.forClass(FlowTimeoutNotification.Message.class);
        verify(channel).send(message.capture());
        assertEquals("RESULT", message.getValue().eventType());
        assertEquals(List.of(8L, 7L), message.getValue().recipients());
        assertEquals("SENT", notice.getState());
    }

    @Test
    void shouldRetryFailureAndStopAfterThirdAttempt() {
        FlowTimeoutNotice notice = prepareNotice(EventType.REMINDER, State.PENDING, 0);
        when(runtime.getById(21L)).thenReturn(instance);
        doThrow(new IllegalStateException("channel failure")).when(channel).send(any());

        for (int attempt = 1; attempt <= 3; attempt++) {
            LocalDateTime retryTime = NOW.plusMinutes((attempt - 1) * 5L);
            service.sendNotification(31L, retryTime);
            assertEquals(attempt, notice.getAttempts());
            assertEquals(attempt == 3 ? "FAILED" : "PENDING", notice.getState());
            assertEquals(retryTime.plusMinutes(5), notice.getRetryAt());
        }
        service.sendNotification(31L, NOW.plusMinutes(15));

        verify(channel, times(3)).send(any());
        verify(dao, times(3)).update(any());
        assertEquals("通知发送失败：IllegalStateException", notice.getError());
    }

    @Test
    void shouldCancelReminderWhenDeadlineReached() {
        FlowTimeoutNotice notice = prepareNotice(EventType.REMINDER, State.PENDING, 0);
        task.setDeadline(NOW);

        service.sendNotification(31L, NOW);

        assertEquals("CANCELLED", notice.getState());
        assertEquals(0, notice.getAttempts());
        verifyNoInteractions(channel);
        verify(dao).update(any());
    }

    @Test
    void shouldCancelReminderWhenTaskCompleted() {
        FlowTimeoutNotice notice = prepareNotice(EventType.REMINDER, State.PENDING, 0);
        task.setStatus(FlowTaskStatusEnum.APPROVE.getCode());

        service.sendNotification(31L, NOW);

        assertEquals("CANCELLED", notice.getState());
        assertEquals(0, notice.getAttempts());
        verifyNoInteractions(channel);
        verify(dao).update(any());
    }

    @ParameterizedTest
    @EnumSource(value = State.class, names = {"SENT", "FAILED", "CANCELLED"})
    void shouldSkipTerminalNoticeStates(State state) {
        FlowTimeoutNotice notice = prepareNotice(EventType.RESULT, state, 1);

        service.sendNotification(31L, NOW);

        assertEquals(state.getCode(), notice.getState());
        assertEquals(1, notice.getAttempts());
        verifyNoInteractions(channel);
        verify(dao, never()).update(any());
    }

    @Test
    void shouldSkipNotificationBeforeRetryTime() {
        FlowTimeoutNotice notice = prepareNotice(EventType.RESULT, State.PENDING, 1);
        notice.setRetryAt(NOW.plusMinutes(5));
        service.sendNotification(31L, NOW);
        assertEquals("PENDING", notice.getState());
        verifyNoInteractions(channel);
        verify(dao, never()).update(any());
    }

    private void prepareDueTask() {
        when(runtime.selectInstanceForUpdate(21L)).thenReturn(instance);
        when(tasks.getById(11L)).thenReturn(task);
    }

    private FlowTimeoutNotice insertedNotice() {
        ArgumentCaptor<FlowTimeoutNotice> captor = ArgumentCaptor.forClass(FlowTimeoutNotice.class);
        verify(dao).insert(captor.capture());
        return captor.getValue();
    }

    private FlowTimeoutNotice prepareNotice(EventType type, State state, int attempts) {
        FlowTimeoutNotice notice = new FlowTimeoutNotice();
        notice.setId(31L);
        notice.setOrgId(9L);
        notice.setInstanceId(21L);
        notice.setTaskId(11L);
        notice.setEventType(type.getCode());
        notice.setState(state.getCode());
        notice.setAttempts(attempts);
        notice.setRetryAt(NOW);
        notice.setCreateTime(NOW);
        when(dao.selectById(31L)).thenReturn(notice);
        when(dao.lock(31L)).thenReturn(notice);
        lenient().when(tasks.getById(11L)).thenReturn(task);
        return notice;
    }
}
