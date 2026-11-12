package com.plasticene.boot.flow.core.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.utils.PtcBeanUtils;
import com.plasticene.boot.flow.core.dao.FlowTaskAddSignDAO;
import com.plasticene.boot.flow.core.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.dao.FlowTimeoutNoticeDAO;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.entity.FlowTimeoutNotice;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.executor.DefaultProcessExecutor;
import com.plasticene.boot.flow.core.model.dto.FlowExtraSetting;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.dto.FlowTimeoutRule;
import com.plasticene.boot.flow.core.model.dto.ProcessNodeCondition;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskReturnParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskTransferParam;
import com.plasticene.boot.flow.core.parser.FlowParser;
import com.plasticene.boot.flow.core.parser.FlowReturnPath;
import com.plasticene.boot.flow.core.provider.FlowTaskAssigneeProvider;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import java.util.ArrayList;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.plasticene.boot.flow.core.event.FlowEventPublisher;
import com.plasticene.boot.flow.core.event.FlowEventType;
import com.plasticene.boot.flow.core.event.FlowTaskAction;
import com.plasticene.boot.flow.core.event.FlowTaskEvent;
import java.util.Collection;
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
 * 以实际任务服务和执行器验证退回后的多轮审批及嵌套并行汇合。
 * @author ZFJ
 * @since 2026-11-02
 */
class FlowTaskReturnTest {
    private LoginUser loginUserContext;
    private final List<FlowTaskEvent> events = new ArrayList<>();
    private final FlowTaskServiceImpl service = new FlowTaskServiceImpl();
    private final DefaultProcessExecutor executor = new DefaultProcessExecutor();
    private final FlowTaskDAO dao = mock(FlowTaskDAO.class);
    private final FlowTimeoutNoticeDAO notices = mock(FlowTimeoutNoticeDAO.class);
    private final FlowRuntimeService runtime = mock(FlowRuntimeService.class);
    private final FlowDefinitionService definitions = mock(FlowDefinitionService.class);
    private final Map<Long, FlowTask> rows = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private final FlowInstance instance = new FlowInstance();
    private final FlowDefinition definition = new FlowDefinition();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        FlowTaskTestComponents.install(service);
        FlowTaskTestComponents.setDependency(service, "flowEventPublisher",
                (FlowEventPublisher) event -> events.add((FlowTaskEvent) event));
        when(runtime.getById(anyLong())).thenReturn(instance);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "return"), FlowTask.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "return"), FlowTimeoutNotice.class);
        instance.setId(1L);
        instance.setOrgId(9L);
        instance.setUserId(99L);
        instance.setDefinitionId(2L);
        instance.setStatus(0);
        instance.setVarMap(Map.of());
        definition.setOrgId(9L);
        definition.setExtraSetting(new FlowExtraSetting());
        login(8L, 9L);
        FlowTaskTestComponents.setDependency(service, "flowTaskDAO", dao);
        FlowTaskTestComponents.setDependency(service, "flowTaskAddSignDAO", mock(FlowTaskAddSignDAO.class));
        FlowTaskTestComponents.setDependency(service, "flowRuntimeService", runtime);
        FlowTaskTestComponents.setDependency(service, "flowDefinitionService", definitions);
        FlowTaskTestComponents.setDependency(service, "flowTimeoutNoticeDAO", notices);
        FlowTaskTestComponents.setDependency(service, "processExecutor", executor);
        var provider = mock(FlowTaskAssigneeProvider.class);
        when(provider.getAssignees(any(), ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong())).thenAnswer(call -> call.<FlowNode>getArgument(0).getAssigneeList());
        FlowTaskTestComponents.setDependency(service, "flowTaskAssigneeProvider", provider);
        ReflectionTestUtils.setField(executor, "flowTaskService", service);
        ReflectionTestUtils.setField(executor, "flowRuntimeService", runtime);
        when(runtime.selectInstanceForUpdate(1L)).thenReturn(instance);
        when(definitions.getById(2L)).thenReturn(definition);
        doAnswer(call -> {
            FlowNode node = call.getArgument(1);
            instance.setCurrentNodeKey(node.getKey());
            return null;
        }).when(runtime).updateInstanceCurrentNode(anyLong(), any());
        doAnswer(call -> { instance.setStatus(1); return null; }).when(runtime).endInstance(any(), any(), any());
        when(dao.selectById(anyLong())).thenAnswer(call -> copy(rows.get(call.<Long>getArgument(0))));
        when(dao.selectList(any(Wrapper.class))).thenAnswer(call -> select(call.getArgument(0)));
        when(dao.selectOne(any(Wrapper.class))).thenAnswer(call -> select(call.getArgument(0)).getFirst());
        when(dao.insert(any(FlowTask.class))).thenAnswer(call -> insert(call.getArgument(0)));
        when(dao.insert(any(Collection.class))).thenAnswer(call -> {
            ((Collection<FlowTask>) call.getArgument(0)).forEach(this::insert);
            return List.of();
        });
        when(dao.update(any(FlowTask.class), any(Wrapper.class))).thenAnswer(call -> {
            FlowTask patch = call.getArgument(0);
            select(call.getArgument(1)).forEach(task -> {
                BeanWrapperImpl source = new BeanWrapperImpl(patch);
                BeanWrapperImpl dest = new BeanWrapperImpl(rows.get(task.getId()));
                for (var property : source.getPropertyDescriptors()) {
                    if (source.isReadableProperty(property.getName()) && dest.isWritableProperty(property.getName())) {
                        Object value = source.getPropertyValue(property.getName());
                        if (value != null) dest.setPropertyValue(property.getName(), value);
                    }
                }
            });
            return 1;
        });
        when(dao.update(any(Wrapper.class))).thenAnswer(call -> {
            LambdaUpdateWrapper<FlowTask> wrapper = call.getArgument(0);
            String setters = wrapper.getSqlSet();
            List<FlowTask> selected = select(wrapper);
            for (FlowTask task : selected) {
                var matcher = Pattern.compile("(\\w+)=(#\\{[^}]+}|null)").matcher(setters);
                while (matcher.find()) {
                    new BeanWrapperImpl(rows.get(task.getId())).setPropertyValue(property(matcher.group(1)), value(wrapper, matcher.group(2)));
                }
            }
            return selected.size();
        });
        doAnswer(call -> {
            String key = call.getArgument(1);
            FlowTask gateway = rows.values().stream().filter(FlowReturnPath::isActive)
                    .filter(task -> key.equals(task.getNodeKey())).findFirst().orElseThrow();
            gateway.setCompletedBranch(gateway.getCompletedBranch() + 1);
            return null;
        }).when(dao).incrementCompletedBranch(anyLong(), anyString());
        doAnswer(call -> {
            String key = call.getArgument(1);
            FlowTask gateway = rows.values().stream().filter(FlowReturnPath::isActive)
                    .filter(task -> task.getIsDelete() == 0 && key.equals(task.getNodeKey())).findFirst().orElseThrow();
            gateway.setCompletedBranch(gateway.getCompletedBranch() + call.<Integer>getArgument(2));
            return null;
        }).when(dao).addCompletedBranches(anyLong(), anyString(), anyInt());
    }

    @Test
    void shouldExecuteConsecutiveAutomaticResultsBeforeWaitingAndReopenAfterReturn() {
        FlowNode first = approval("first");
        first.setApproveType(FlowNodeEnum.ApproveType.AUTO_PASS.getCode());
        FlowNode empty = approval("empty");
        empty.setAssigneeList(List.of());
        empty.setAssigneeEmpty(FlowNodeEnum.AssigneeEmpty.AUTO_APPROVE.getCode());
        FlowNode self = approval("self");
        self.setAssigneeList(List.of(99L));
        self.setSelfApprove(FlowNodeEnum.SelfApprove.AUTO_SKIP.getCode());
        FlowNode manual = approval("manual");
        FlowNode last = approval("last");
        first.setChildNode(empty);
        empty.setChildNode(self);
        self.setChildNode(manual);
        manual.setChildNode(last);
        start(first);
        assertEquals(List.of("manual"), pendingKeys());
        assertEquals(List.of("first", "empty", "self", "manual"),
                events.stream().map(FlowTaskEvent::nodeKey).toList());
        assertEquals(List.of(FlowEventType.TASK_COMPLETED, FlowEventType.TASK_COMPLETED,
                FlowEventType.TASK_COMPLETED, FlowEventType.TASK_CREATED),
                events.stream().map(FlowTaskEvent::eventType).toList());
        assertTrue(events.stream().allMatch(event -> rows.containsKey(event.taskId())));
        approve("manual");
        back("last", "manual");
        assertEquals(List.of("manual"), pendingKeys());
        approve("manual");
        assertEquals(List.of("last"), pendingKeys());
        assertEquals(3, rows.values().stream().filter(task -> task.getApproveType() == 1).count());
    }

    @Test
    void shouldPublishAutomaticRejectionBeforeEndingInstance() {
        FlowNode empty = approval("empty");
        empty.setAssigneeList(List.of());
        empty.setAssigneeEmpty(FlowNodeEnum.AssigneeEmpty.AUTO_REJECT.getCode());
        empty.setChildNode(approval("unreachable"));
        doAnswer(call -> {
            assertEquals(1, events.size());
            assertEquals(FlowEventType.TASK_COMPLETED, events.getFirst().eventType());
            assertEquals(FlowTaskAction.REJECT, events.getFirst().action());
            assertEquals(2, rows.values().iterator().next().getStatus());
            instance.setStatus(2);
            return null;
        }).when(runtime).endInstance(any(), any(), any());
        start(empty);
        assertEquals(List.of(), pendingKeys());
        assertEquals(2, instance.getStatus());
        verify(runtime).endInstance(instance, empty,
                FlowInstanceStatusEnum.REJECT);
    }

    @Test
    void shouldPublishReturnCompletionAndReopenedTaskWithoutCancellingCompletedHistory() {
        FlowNode a = approval("a"), b = approval("b");
        a.setChildNode(b);
        start(a);
        Long first = pending("a").getId();
        approve("a");
        Long second = pending("b").getId();
        events.clear();
        back("b", "a");
        assertEquals(List.of(FlowEventType.TASK_COMPLETED, FlowEventType.TASK_CREATED),
                events.stream().map(FlowTaskEvent::eventType).toList());
        assertEquals(second, events.getFirst().taskId());
        assertEquals(FlowTaskAction.RETURN, events.getFirst().action());
        assertEquals("a", events.getLast().nodeKey());
        assertNotEquals(first, events.getLast().taskId());
    }

    @Test
    void serialReturnMustCreateNewTasksAndRetainHistoryAcrossTwoRounds() {
        FlowNode a = approval("a"), b = approval("b"), c = approval("c");
        a.setChildNode(b); b.setChildNode(c); start(a);
        approve("a"); approve("b");
        Long oldC = pending("c").getId();
        clearInvocations(runtime);
        assertEquals(List.of("a", "b"), service.listReturnTargets(oldC, loginUserContext).stream().map(t -> t.getNodeKey()).toList());
        verify(runtime, never()).selectInstanceForUpdate(anyLong());
        back("c", "a");
        assertEquals(5, rows.get(oldC).getStatus());
        assertEquals("补充依据", rows.get(oldC).getComment());
        assertEquals("a", rows.get(oldC).getReturnTargetNodeKey());
        assertEquals(List.of("a"), pendingKeys());
        assertTrue(rows.values().stream().filter(t -> t.getId() <= oldC).allMatch(t -> t.getReturnTaskId().equals(oldC)));
        approve("a"); approve("b"); back("c", "b");
        assertEquals(List.of("b"), pendingKeys());
        approve("b"); assertEquals(List.of("c"), pendingKeys());
        verify(notices, atLeastOnce()).update(any(Wrapper.class));
    }

    @Test
    void shouldRejectReturnAndTargetQueryWhenPublishedNodeDisablesReturn() {
        FlowNode a = approval("a"), b = approval("b");
        b.setAllowReturn(false);
        a.setChildNode(b);
        start(a);
        approve("a");
        Long taskId = pending("b").getId();

        assertEquals("当前审批节点不允许退回", assertThrows(BizException.class,
                () -> service.listReturnTargets(taskId, loginUserContext)).getMessage());
        assertEquals("当前审批节点不允许退回", assertThrows(BizException.class,
                () -> back("b", "a")).getMessage());
        assertEquals(List.of("b"), pendingKeys());
        assertEquals(0, rows.get(taskId).getStatus());
    }

    @Test
    void shouldAllowReturnWithTransferAndAddSignDisabled() {
        FlowNode a = approval("a"), b = approval("b");
        b.setAllowTransfer(false);
        b.setAllowAddSign(false);
        b.setAllowReturn(true);
        a.setChildNode(b);
        start(a);
        approve("a");

        assertEquals(List.of("a"), service.listReturnTargets(pending("b").getId(), loginUserContext).stream()
                .map(target -> target.getNodeKey()).toList());
        back("b", "a");
        assertEquals(List.of("a"), pendingKeys());
    }

    @Test
    void returnAfterJoinMustPreserveSiblingAndSkippedBranchContribution() {
        FlowNode b = approval("b"), c = approval("c"), d = approval("d");
        FlowNode gateway = parallel("g", branch("left", b), branch("right", c));
        gateway.setChildNode(d); start(gateway);
        approve("b"); approve("c"); back("d", "b");
        assertEquals(List.of("b"), pendingKeys());
        assertTrue(active("c").stream().anyMatch(t -> t.getStatus() == 1));
        assertEquals(1, active("g").getFirst().getCompletedBranch());
        approve("b"); assertEquals(List.of("d"), pendingKeys());
        assertEquals(2, active("g").getFirst().getCompletedBranch());
    }

    @Test
    void returnInsideBranchMustLeaveOtherRunningBranchUntouched() {
        FlowNode b = approval("b"), next = approval("next"), c = approval("c"); b.setChildNode(next);
        start(parallel("g", branch("left", b), branch("right", c)));
        approve("b"); Long sibling = pending("c").getId();
        assertFalse(service.listReturnTargets(pending("next").getId(), loginUserContext).stream().anyMatch(t -> t.getNodeKey().equals("c")));
        back("next", "b");
        assertEquals(sibling, pending("c").getId());
        assertEquals(0, active("g").getFirst().getCompletedBranch());
        approve("b"); approve("next");
        assertEquals(List.of("c"), pendingKeys());
        approve("c"); assertEquals(2, active("g").getFirst().getCompletedBranch());
    }

    @Test
    void returnBeforeForkMustRecreateAllBranches() {
        FlowNode a = approval("a"), b = approval("b"), c = approval("c");
        a.setChildNode(parallel("g", branch("left", b), branch("right", c))); start(a);
        approve("a"); Long oldC = pending("c").getId(); back("b", "a");
        assertEquals(3, rows.get(oldC).getStatus());
        assertEquals(List.of("a"), pendingKeys());
        approve("a"); assertEquals(List.of("b", "c"), pendingKeys());
        approve("b"); approve("c"); assertEquals(2, active("g").getFirst().getCompletedBranch());
    }

    @Test
    void nestedReturnMustResetEachCompletedAncestorOnlyOnce() {
        FlowNode b = approval("b"), c = approval("c"), other = approval("other"), d = approval("d");
        FlowNode inner = parallel("inner", branch("i1", b), branch("i2", c));
        FlowNode outer = parallel("outer", branch("o1", inner), branch("o2", other));
        outer.setChildNode(d); start(outer);
        approve("b"); approve("c"); approve("other"); back("d", "b");
        assertEquals(1, active("inner").getFirst().getCompletedBranch());
        assertEquals(1, active("outer").getFirst().getCompletedBranch());
        approve("b");
        assertEquals(2, active("inner").getFirst().getCompletedBranch());
        assertEquals(2, active("outer").getFirst().getCompletedBranch());
        assertEquals(List.of("d"), pendingKeys());
    }

    @Test
    void emptyParallelSiblingMustRemainCompletedWhenReturningAfterJoin() {
        FlowNode b = approval("b"), d = approval("d");
        FlowNode gateway = parallel("g", branch("left", b), branch("empty", null)); gateway.setChildNode(d);
        start(gateway); approve("b"); back("d", "b");
        assertEquals(1, active("g").getFirst().getCompletedBranch());
        approve("b"); assertEquals(List.of("d"), pendingKeys());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void returnMustRestartAllCountersignOrOrderedParticipants(int mode) {
        FlowNode a = approval("a"), b = approval("b");
        a.setApproveMode(mode); a.setAssigneeList(List.of(8L, 10L)); a.setChildNode(b); start(a);
        approve("a");
        if (mode != 1) { login(10L, 9L); approve("a"); login(8L, 9L); }
        back("b", "a");
        assertEquals(mode == 2 ? 1 : 2, active("a").stream().filter(t -> t.getStatus() == 0).count());
        assertEquals(8L, pending("a").getAssignee());
    }

    @Test
    void oldApprovalMustNotDeduplicateNewRoundAndTimeoutMustRestart() {
        FlowNode a = approval("a"), b = approval("b"); a.setChildNode(b);
        FlowTimeoutRule rule = new FlowTimeoutRule(); rule.setAutoEnabled(true); rule.setAutoAfter(10); rule.setAutoUnit(FlowTimeoutRule.Unit.MINUTE);
        rule.setAction(FlowTimeoutRule.Action.APPROVE);
        definition.getExtraSetting().setTimeoutRule(rule);
        start(a); FlowTask original = pending("a"); approve("a");
        definition.getExtraSetting().setApproverDeduplication(FlowExtraSetting.ApproverDeduplication.ONCE_PER_PROCESS);
        back("b", "a");
        FlowTask restarted = pending("a");
        assertNotEquals(original.getId(), restarted.getId());
        assertFalse(restarted.getDeadline().isBefore(original.getDeadline()));
        assertEquals(0, restarted.getRemindCount());
        assertFalse(restarted.getTimeoutHandled());
    }

    @Test
    void invalidTargetsBlankReasonsAndForeignUsersMustNotMutateTasks() {
        FlowNode a = approval("a"), b = approval("b"); a.setChildNode(b); start(a); approve("a");
        for (String key : List.of("b", "missing")) assertThrows(BizException.class, () -> back("b", key));
        FlowTaskReturnParam param = request(pending("b").getId(), "a"); param.setComment("   ");
        assertThrows(BizException.class, () -> service.returnTask(param, loginUserContext));
        param.setComment("x".repeat(501)); assertThrows(BizException.class, () -> service.returnTask(param, loginUserContext));
        login(8L, 12L); assertThrows(BizException.class, () -> service.returnTask(request(pending("b").getId(), "a"), loginUserContext));
        login(10L, 9L); assertThrows(BizException.class, () -> service.listReturnTargets(pending("b").getId(), loginUserContext));
        assertEquals(List.of("b"), pendingKeys());
        assertTrue(rows.values().stream().allMatch(FlowReturnPath::isActive));
    }

    @Test
    void staleTaskMustRejectApproveTransferWithdrawAndReturn() {
        FlowNode a = approval("a"), b = approval("b"); a.setChildNode(b); start(a); approve("a");
        FlowTask stale = pending("b"); back("b", "a");
        var action = new FlowTaskParam(); action.setTaskId(stale.getId());
        assertThrows(BizException.class, () -> service.approveTask(action, loginUserContext));
        assertThrows(BizException.class, () -> service.rejectTask(action, loginUserContext));
        var transfer = new FlowTaskTransferParam(); transfer.setTaskId(stale.getId());
        assertThrows(BizException.class, () -> service.transferTask(transfer, loginUserContext));
        assertThrows(BizException.class, () -> service.withdrawTask(stale.getId(), loginUserContext));
        assertThrows(BizException.class, () -> service.returnTask(request(stale.getId(), "a"), loginUserContext));
    }

    @Test
    void lockWaitMustRecheckTaskBeforeApplyingReturn() {
        FlowNode a = approval("a"), b = approval("b"); a.setChildNode(b); start(a); approve("a");
        FlowTask task = pending("b");
        when(runtime.selectInstanceForUpdate(1L)).thenAnswer(call -> {
            rows.get(task.getId()).setStatus(1); return instance;
        });
        assertThrows(BizException.class, () -> service.returnTask(request(task.getId(), "a"), loginUserContext));
        assertTrue(rows.values().stream().allMatch(FlowReturnPath::isActive));
    }

    @Test
    void skippedConditionalBranchesAndAutomaticNodesMustNotBeTargets() {
        FlowNode a = approval("a"), skipped = approval("skipped"), d = approval("d");
        FlowNode automatic = approval("automatic"); automatic.setApproveType(1); a.setChildNode(automatic);
        FlowNode gateway = node("condition", FlowNodeEnum.Type.CONDITION_BRANCH);
        gateway.setConditionNodes(List.of(branch("chosen", a), branch("unchosen", skipped)));
        gateway.setChildNode(d); start(gateway); approve("a");
        assertEquals(List.of("a"), service.listReturnTargets(pending("d").getId(), loginUserContext).stream().map(t -> t.getNodeKey()).toList());
        assertThrows(BizException.class, () -> back("d", "skipped"));
        assertThrows(BizException.class, () -> back("d", "automatic"));
    }

    @Test
    void skippedParallelBranchMustNotBeReevaluatedDuringPartialReturn() {
        FlowNode b = approval("b"), d = approval("d");
        FlowNode skipped = branch("skipped", approval("never"));
        var condition = new ProcessNodeCondition();
        condition.setIsDefault(true); skipped.setCondition(condition);
        FlowNode gateway = parallel("g", branch("left", b), skipped); gateway.setChildNode(d);
        start(gateway); approve("b"); back("d", "b");
        assertEquals(1, active("g").getFirst().getCompletedBranch());
        approve("b"); assertEquals(List.of("d"), pendingKeys());
        assertTrue(active("never").isEmpty());
    }

    @Test
    void returnMustResolveConfiguredAssigneesInsteadOfPreviousTransferRecipient() {
        FlowNode a = approval("a"), b = approval("b"); a.setChildNode(b); start(a);
        // 模拟上一轮转交后由接收人审批的有效结果。
        FlowTask original = pending("a"); rows.get(original.getId()).setAssignee(10L);
        login(10L, 9L); approve("a"); login(8L, 9L); back("b", "a");
        assertEquals(8L, pending("a").getAssignee());
    }

    @Test
    @SuppressWarnings("unchecked")
    void transactionMustRestoreHistoryAndGatewayWhenTargetCreationFails() {
        FlowNode b = approval("b"), c = approval("c"), d = approval("d");
        FlowNode gateway = parallel("g", branch("left", b), branch("right", c)); gateway.setChildNode(d);
        start(gateway); approve("b"); approve("c");
        Long sourceId = pending("d").getId();
        Map<Long, FlowTask> snapshot = new LinkedHashMap<>(); rows.forEach((id, task) -> snapshot.put(id, copy(task)));
        var manager = new AbstractPlatformTransactionManager() {
            protected Object doGetTransaction() { return new Object(); }
            protected void doBegin(Object transaction, TransactionDefinition def) { }
            protected void doCommit(DefaultTransactionStatus status) { }
            protected void doRollback(DefaultTransactionStatus status) {
                rows.clear(); snapshot.forEach((id, task) -> rows.put(id, copy(task)));
            }
        };
        var proxy = new ProxyFactory(service);
        proxy.addAdvice(new TransactionInterceptor(manager,
                new AnnotationTransactionAttributeSource()));
        var transactional = (FlowTaskService) proxy.getProxy();
        when(dao.insert(any(Collection.class))).thenThrow(new IllegalStateException("creation failure"));
        assertThrows(IllegalStateException.class, () -> transactional.returnTask(request(sourceId, "b"), loginUserContext));
        assertEquals(snapshot, rows);
        assertEquals(List.of("d"), pendingKeys());
        assertEquals(2, active("g").getFirst().getCompletedBranch());
    }

    private void start(FlowNode root) {
        definition.setModelNode(root); FlowParser.makeParentNode(root); executor.executeNode(instance, root);
    }
    private void approve(String key) {
        FlowTaskParam param = new FlowTaskParam();
        param.setTaskId(active(key).stream().filter(t -> t.getStatus() == 0 && Objects.equals(t.getAssignee(), loginUserContext.getId())).findFirst().orElseThrow().getId());
        service.approveTask(param, loginUserContext);
    }
    private void back(String from, String to) { service.returnTask(request(pending(from).getId(), to), loginUserContext); }
    private FlowTaskReturnParam request(Long taskId, String key) {
        var param = new FlowTaskReturnParam(); param.setTaskId(taskId); param.setTargetNodeKey(key); param.setComment("  补充依据  "); return param;
    }
    private List<FlowTask> active(String key) {
        return rows.values().stream().filter(FlowReturnPath::isActive).filter(t -> t.getIsDelete() == 0 && key.equals(t.getNodeKey())).toList();
    }
    private FlowTask pending(String key) { return active(key).stream().filter(t -> t.getStatus() == 0).findFirst().orElseThrow(); }
    private List<String> pendingKeys() {
        return rows.values().stream().filter(FlowReturnPath::isActive).filter(t -> t.getIsDelete() == 0 && t.getStatus() == 0).map(FlowTask::getNodeKey).distinct().sorted().toList();
    }
    private FlowNode approval(String key) {
        FlowNode node = node(key, FlowNodeEnum.Type.APPROVE); node.setApproveType(0); node.setApproveMode(0); node.setSelfApprove(0); node.setAssigneeList(List.of(8L)); return node;
    }
    private FlowNode branch(String key, FlowNode child) { FlowNode node = node(key, FlowNodeEnum.Type.CONDITION_NODE); node.setChildNode(child); return node; }
    private FlowNode parallel(String key, FlowNode... branches) { FlowNode node = node(key, FlowNodeEnum.Type.PARALLEL_BRANCH); node.setConditionNodes(List.of(branches)); return node; }
    private FlowNode node(String key, FlowNodeEnum.Type type) { FlowNode node = new FlowNode(); node.setKey(key); node.setName(key); node.setType(type.getCode()); return node; }
    private void login(Long id, Long org) { LoginUser user = new LoginUser(); user.setId(id); user.setOrgId(org); loginUserContext = user; }
    private FlowTask copy(FlowTask task) { return task == null ? null : PtcBeanUtils.copy(task, FlowTask.class); }
    private int insert(FlowTask task) {
        task.setId(sequence.incrementAndGet()); if (task.getIsDelete() == null) task.setIsDelete(0); rows.put(task.getId(), copy(task)); return 1;
    }

    // Mapper替身执行服务实际生成的等值、IN和NULL条件，验证多轮真实流转而不连接外部数据库。
    private List<FlowTask> select(Wrapper<FlowTask> wrapper) {
        String sql = wrapper.getSqlSegment().split("ORDER BY")[0];
        var pattern = Pattern.compile("(\\w+) (IS NULL|IN \\([^)]+\\)|= #\\{[^}]+})");
        return rows.values().stream().filter(task -> {
            var matcher = pattern.matcher(sql);
            while (matcher.find()) {
                Object actual = new BeanWrapperImpl(task).getPropertyValue(property(matcher.group(1)));
                String condition = matcher.group(2);
                if (condition.equals("IS NULL")) { if (actual != null) return false; }
                else if (condition.startsWith("IN")) {
                    var values = Pattern.compile("#\\{[^}]+}").matcher(condition); boolean found = false;
                    while (values.find()) found |= Objects.equals(actual, value(wrapper, values.group()));
                    if (!found) return false;
                } else if (!Objects.equals(actual, value(wrapper, condition.substring(2)))) return false;
            }
            return true;
        }).map(this::copy).toList();
    }
    private Object value(Wrapper<FlowTask> wrapper, String placeholder) {
        if (placeholder.equals("null")) return null;
        String name = placeholder.substring(placeholder.lastIndexOf('.') + 1, placeholder.length() - 1);
        return ((AbstractWrapper<?, ?, ?>) wrapper).getParamNameValuePairs().get(name);
    }
    private String property(String column) {
        StringBuilder result = new StringBuilder(); boolean upper = false;
        for (char c : column.toCharArray()) { if (c == '_') upper = true; else { result.append(upper ? Character.toUpperCase(c) : c); upper = false; } }
        return result.toString();
    }
}
