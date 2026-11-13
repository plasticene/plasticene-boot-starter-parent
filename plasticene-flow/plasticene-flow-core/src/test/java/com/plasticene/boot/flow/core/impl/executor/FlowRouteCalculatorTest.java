package com.plasticene.boot.flow.core.impl.executor;

import com.plasticene.boot.flow.core.model.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.model.FlowNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证路径预测的遍历顺序、路由节点展示与无匹配时的后续路径。
 * @author ZFJ
 * @since 2026-11-10
 */
class FlowRouteCalculatorTest {
    private final FlowRouteCalculator calculator = new FlowRouteCalculator(new FlowBranchSelector());

    @Test
    void shouldReturnEmptyRouteForMissingRoot() {
        assertEquals(List.of(), calculator.calculateRoute(null, null));
        assertEquals(List.of(), calculator.calculateRouteTrace(null, null));
    }

    @Test
    void shouldVisitOrdinaryNodesInOrderAndReturnIndependentResults() {
        FlowNode start = node("start", FlowNodeEnum.Type.START);
        FlowNode approve = node("approve", FlowNodeEnum.Type.APPROVE);
        FlowNode copy = node("copy", FlowNodeEnum.Type.COPY);
        FlowNode end = node("end", FlowNodeEnum.Type.END);
        start.setChildNode(approve);
        approve.setChildNode(copy);
        copy.setChildNode(end);
        List<FlowNode> expected = List.of(start, approve, copy, end);
        assertEquals(expected, calculator.calculateRoute(start, Map.of()));
        assertEquals(expected, calculator.calculateRouteTrace(start, Map.of()));
        calculator.calculateRoute(start, Map.of()).clear();
        assertEquals(expected, calculator.calculateRoute(start, Map.of()));
        assertNull(approve.getParentNode());
    }

    @Test
    void shouldIncludeNestedRoutingNodesAndVisitGatewayContinuationOnlyOnce() {
        FlowNode parallel = node("parallel", FlowNodeEnum.Type.PARALLEL_BRANCH);
        FlowNode left = node("left", FlowNodeEnum.Type.CONDITION_NODE);
        FlowNode right = node("right", FlowNodeEnum.Type.CONDITION_NODE);
        FlowNode exclusive = node("exclusive", FlowNodeEnum.Type.CONDITION_BRANCH);
        FlowNode selected = node("selected", FlowNodeEnum.Type.CONDITION_NODE);
        FlowNode skipped = node("skipped", FlowNodeEnum.Type.CONDITION_NODE);
        FlowNode approve = node("approve", FlowNodeEnum.Type.APPROVE);
        FlowNode copy = node("copy", FlowNodeEnum.Type.COPY);
        FlowNode end = node("end", FlowNodeEnum.Type.END);
        parallel.setConditionNodes(List.of(left, right));
        parallel.setChildNode(end);
        left.setChildNode(exclusive);
        exclusive.setConditionNodes(List.of(selected, skipped));
        selected.setChildNode(approve);
        right.setChildNode(copy);
        assertEquals(List.of(approve, copy, end), calculator.calculateRoute(parallel, Map.of()));
        assertEquals(List.of(parallel, left, exclusive, selected, approve, right, copy, end),
                calculator.calculateRouteTrace(parallel, Map.of()));
        assertNull(selected.getParentNode());
    }

    @Test
    void shouldPredictContinuationWhenGatewayHasNoSelectedBranch() {
        for (FlowNodeEnum.Type type : List.of(FlowNodeEnum.Type.CONDITION_BRANCH, FlowNodeEnum.Type.PARALLEL_BRANCH)) {
            FlowNode gateway = node("gateway", type);
            gateway.setConditionNodes(List.of());
            FlowNode end = node("end", FlowNodeEnum.Type.END);
            gateway.setChildNode(end);
            assertEquals(List.of(end), calculator.calculateRoute(gateway, Map.of()));
            assertEquals(List.of(gateway, end), calculator.calculateRouteTrace(gateway, Map.of()));
        }
    }

    private FlowNode node(String key, FlowNodeEnum.Type type) {
        FlowNode node = new FlowNode();
        node.setKey(key);
        node.setType(type.getCode());
        return node;
    }
}
