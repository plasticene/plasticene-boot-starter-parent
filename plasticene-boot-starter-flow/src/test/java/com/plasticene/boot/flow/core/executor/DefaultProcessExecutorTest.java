package com.plasticene.boot.flow.core.executor;

import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
