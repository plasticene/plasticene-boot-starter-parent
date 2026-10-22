package com.plasticene.boot.flow.core.executor;

import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.model.dto.ProcessNodeCondition;
import com.plasticene.boot.flow.core.model.dto.ProcessConditionGroup;
import com.plasticene.boot.flow.core.model.dto.ProcessConditionRule;
import com.plasticene.boot.flow.core.parser.FlowParser;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * 流程预计路径计算测试
 *
 * @author ZFJ
 * @since 2026-09-14
 */
class DefaultProcessExecutorTest {

    private final ProcessExecutor processExecutor = new DefaultProcessExecutor();

    @Test
    void shouldIncludeMatchedRoutingNodesOnlyInRouteTrace() {
        FlowNode start = node("start", FlowNodeEnum.Type.START);
        FlowNode gateway = node("gateway", FlowNodeEnum.Type.CONDITION_BRANCH);
        FlowNode matchedCondition = node("condition-match", FlowNodeEnum.Type.CONDITION_NODE);
        FlowNode skippedCondition = node("condition-skip", FlowNodeEnum.Type.CONDITION_NODE);
        FlowNode approve = node("approve", FlowNodeEnum.Type.APPROVE);
        FlowNode end = node("end", FlowNodeEnum.Type.END);
        start.setChildNode(gateway);
        gateway.setConditionNodes(List.of(matchedCondition, skippedCondition));
        gateway.setChildNode(end);
        matchedCondition.setChildNode(approve);

        List<String> regularRoute = keys(processExecutor.calculateRoute(start, Map.of()));
        List<String> routeTrace = keys(processExecutor.calculateRouteTrace(start, Map.of()));

        assertEquals(List.of("start", "approve", "end"), regularRoute);
        assertEquals(List.of("start", "gateway", "condition-match", "approve", "end"), routeTrace);
    }

    @ParameterizedTest
    @CsvSource({"yes,yes,a|b", "yes,no,a", "no,yes,b", "no,no,fallback"})
    void shouldUseSameSelectionForPreviewAndExecution(String a, String b, String expected) {
        FlowNode first = conditional("a", "a");
        FlowNode second = conditional("b", "b");
        FlowNode gateway = gateway("gateway", first, second, fallback());
        Map<String, Object> variables = Map.of("a", a, "b", b);
        List<String> selected = List.of(expected.split("\\|"));
        List<String> trace = keys(processExecutor.calculateRouteTrace(gateway, variables));
        assertEquals(selected, trace.subList(1, trace.size() - 1));
        Harness harness = new Harness(variables);
        harness.execute(gateway);
        for (FlowNode branch : gateway.getConditionNodes()) {
            verify(harness.tasks, times(selected.contains(branch.getKey()) ? 1 : 0))
                    .createConditionNodeTask(harness.instance, branch);
        }
        verify(harness.tasks, times(1)).createEndTask(harness.instance, gateway.getChildNode());
    }

    @Test
    void shouldExecuteAllUnconditionalBranchesAndNeverFallback() {
        FlowNode a = node("a", FlowNodeEnum.Type.CONDITION_NODE);
        FlowNode b = conditional("b", "b");
        FlowNode c = node("c", FlowNodeEnum.Type.CONDITION_NODE);
        FlowNode gateway = gateway("gateway", a, b, c, fallback());
        Harness harness = new Harness(Map.of("b", "no"));
        harness.execute(gateway);
        assertEquals(List.of("gateway", "a", "c", "gateway-end"),
                keys(processExecutor.calculateRouteTrace(gateway, harness.instance.getVarMap())));
        verify(harness.tasks).createConditionNodeTask(harness.instance, a);
        verify(harness.tasks).createConditionNodeTask(harness.instance, c);
        verify(harness.tasks, times(1)).createEndTask(any(), any());
    }

    @Test
    void shouldWaitForAllSelectedManualBranchesWithoutReevaluatingVariables() {
        FlowNode a = conditional("a", "a");
        FlowNode b = conditional("b", "b");
        FlowNode first = node("first", FlowNodeEnum.Type.APPROVE);
        FlowNode second = node("second", FlowNodeEnum.Type.APPROVE);
        a.setChildNode(first);
        b.setChildNode(second);
        FlowNode gateway = gateway("gateway", a, b, fallback());
        Harness harness = new Harness(new HashMap<>(Map.of("a", "yes", "b", "yes")));
        harness.execute(gateway);
        verify(harness.tasks, never()).createEndTask(any(), any());
        harness.instance.getVarMap().put("b", "no");
        processExecutor.moveToNextNode(harness.instance, first);
        verify(harness.tasks, never()).createEndTask(any(), any());
        processExecutor.moveToNextNode(harness.instance, second);
        verify(harness.tasks, times(1)).createEndTask(any(), any());
    }

    @Test
    void shouldJoinConcurrentManualCompletionsOnce() throws Exception {
        FlowNode a = node("a", FlowNodeEnum.Type.CONDITION_NODE);
        FlowNode b = node("b", FlowNodeEnum.Type.CONDITION_NODE);
        a.setChildNode(node("first", FlowNodeEnum.Type.APPROVE));
        b.setChildNode(node("second", FlowNodeEnum.Type.APPROVE));
        FlowNode gateway = gateway("gateway", a, b, fallback());
        Harness harness = new Harness(Map.of());
        harness.execute(gateway);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var one = pool.submit(() -> processExecutor.moveToNextNode(harness.instance, a.getChildNode()));
            var two = pool.submit(() -> processExecutor.moveToNextNode(harness.instance, b.getChildNode()));
            one.get();
            two.get();
        }
        verify(harness.tasks, times(1)).createEndTask(any(), any());
    }

    @Test
    void shouldJoinNestedGatewaysWithoutAChildAndWaitForOuterSibling() {
        FlowNode left = node("left", FlowNodeEnum.Type.CONDITION_NODE);
        FlowNode right = node("right", FlowNodeEnum.Type.CONDITION_NODE);
        FlowNode innerBranch = node("inner-branch", FlowNodeEnum.Type.CONDITION_NODE);
        FlowNode auto = node("auto", FlowNodeEnum.Type.APPROVE);
        auto.setApproveType(FlowNodeEnum.ApproveType.AUTO_PASS.getCode());
        innerBranch.setChildNode(auto);
        FlowNode inner = gateway("inner", innerBranch, fallback());
        inner.setChildNode(null);
        left.setChildNode(inner);
        FlowNode manual = node("manual", FlowNodeEnum.Type.APPROVE);
        right.setChildNode(manual);
        FlowNode outer = gateway("outer", left, right, fallback());
        Harness harness = new Harness(Map.of());
        harness.execute(outer);
        assertSame(innerBranch, auto.getParentNode());
        assertSame(inner, innerBranch.getParentNode());
        assertSame(innerBranch, FlowParser.findNodeByKey(outer, "inner-branch"));
        verify(harness.tasks, never()).createEndTask(any(), any());
        processExecutor.moveToNextNode(harness.instance, manual);
        verify(harness.tasks, times(1)).createEndTask(any(), any());
        assertNull(FlowParser.findExecutionNextNode(outer.getChildNode()));
    }

    @Test
    void shouldJoinExclusiveGatewayInsideParallelBranchOnce() {
        FlowNode left = node("left", FlowNodeEnum.Type.CONDITION_NODE);
        FlowNode right = node("right", FlowNodeEnum.Type.CONDITION_NODE);
        FlowNode exclusive = node("exclusive", FlowNodeEnum.Type.CONDITION_BRANCH);
        exclusive.setConditionNodes(List.of(fallback()));
        left.setChildNode(exclusive);
        FlowNode outer = gateway("outer", left, right, fallback());
        Harness harness = new Harness(Map.of());
        harness.execute(outer);
        verify(harness.tasks, times(1)).createEndTask(any(), any());
    }

    @Test
    void shouldStopRemainingBranchesAfterAutomaticRejection() {
        FlowNode left = node("left", FlowNodeEnum.Type.CONDITION_NODE);
        FlowNode right = node("right", FlowNodeEnum.Type.CONDITION_NODE);
        FlowNode reject = node("reject", FlowNodeEnum.Type.APPROVE);
        reject.setApproveType(FlowNodeEnum.ApproveType.AUTO_REJECT.getCode());
        left.setChildNode(reject);
        FlowNode outer = gateway("outer", left, right, fallback());
        Harness harness = new Harness(Map.of());
        harness.execute(outer);
        verify(harness.tasks, never()).createConditionNodeTask(harness.instance, right);
        verify(harness.tasks, never()).createEndTask(any(), any());
        assertEquals(FlowInstanceStatusEnum.REJECT.getCode(), harness.instance.getStatus());
    }

    @Test
    void shouldCollectNestedConditionFieldsInsideUnconditionalBranchesWithoutDuplicates() {
        FlowNode ordinary = node("ordinary", FlowNodeEnum.Type.CONDITION_NODE);
        FlowNode inner = gateway("inner", conditional("a", "amount"), conditional("b", "amount"), fallback());
        inner.setChildNode(null);
        ordinary.setChildNode(inner);
        FlowNode outer = gateway("outer", ordinary, fallback());
        assertEquals(List.of("amount"), FlowParser.getAllProcessConditionRules(outer).stream()
                .map(ProcessConditionRule::getField).toList());
    }

    private FlowNode conditional(String key, String field) {
        FlowNode branch = node(key, FlowNodeEnum.Type.CONDITION_NODE);
        ProcessConditionRule rule = new ProcessConditionRule();
        rule.setSource(0);
        rule.setType(0);
        rule.setField(field);
        rule.setOperator("=");
        rule.setInputValue("yes");
        ProcessConditionGroup group = new ProcessConditionGroup();
        group.setType(0);
        group.setConditionRules(List.of(rule));
        ProcessNodeCondition condition = new ProcessNodeCondition();
        condition.setType(0);
        condition.setConditionGroups(List.of(group));
        branch.setCondition(condition);
        return branch;
    }

    private FlowNode fallback() {
        FlowNode branch = node("fallback", FlowNodeEnum.Type.CONDITION_NODE);
        ProcessNodeCondition condition = new ProcessNodeCondition();
        condition.setIsDefault(true);
        branch.setCondition(condition);
        return branch;
    }

    private FlowNode gateway(String key, FlowNode... branches) {
        FlowNode gateway = node(key, FlowNodeEnum.Type.PARALLEL_BRANCH);
        gateway.setConditionNodes(List.of(branches));
        gateway.setChildNode(node(key + "-end", FlowNodeEnum.Type.END));
        return gateway;
    }

    private class Harness {
        final FlowTaskService tasks = mock(FlowTaskService.class);
        final FlowRuntimeService runtime = mock(FlowRuntimeService.class);
        final FlowInstance instance = new FlowInstance();
        final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

        Harness(Map<String, Object> variables) {
            instance.setId(1L);
            instance.setStatus(FlowInstanceStatusEnum.RUNNING.getCode());
            instance.setVarMap(variables);
            ReflectionTestUtils.setField(processExecutor, "flowTaskService", tasks);
            ReflectionTestUtils.setField(processExecutor, "flowRuntimeService", runtime);
            doAnswer(call -> {
                FlowNode node = call.getArgument(1);
                counters.put(node.getKey(), new AtomicInteger());
                return null;
            }).when(tasks).createBranchTask(any(), any());
            when(tasks.incrementCompletedBranchAndGet(anyLong(), anyString())).thenAnswer(call ->
                    counters.get(call.<String>getArgument(1)).incrementAndGet());
            doAnswer(call -> {
                FlowInstanceStatusEnum status = call.getArgument(2);
                instance.setStatus(status.getCode());
                return null;
            }).when(runtime).endInstance(any(), any(), any());
        }

        void execute(FlowNode gateway) {
            FlowParser.makeParentNode(gateway);
            processExecutor.executeNode(instance, gateway);
        }
    }

    private FlowNode node(String key, FlowNodeEnum.Type type) {
        FlowNode node = new FlowNode();
        node.setKey(key);
        node.setName(key);
        node.setType(type.getCode());
        return node;
    }

    private List<String> keys(List<FlowNode> nodes) {
        return nodes.stream().map(FlowNode::getKey).toList();
    }
}
