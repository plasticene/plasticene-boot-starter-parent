package com.plasticene.boot.flow.core.impl.executor;

import com.plasticene.boot.flow.core.model.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.model.FlowNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 根据变量预测流程路径，复用实际执行时的分支选择规则。
 * @author ZFJ
 * @since 2026-11-10
 */
final class FlowRouteCalculator {
    private final FlowBranchSelector branchSelector;

    FlowRouteCalculator(FlowBranchSelector branchSelector) {
        this.branchSelector = branchSelector;
    }

    public List<FlowNode> calculateRoute(FlowNode flowNode, Map<String, Object> varMap) {
        return calculateRoute(flowNode, varMap, false);
    }

    public List<FlowNode> calculateRouteTrace(FlowNode flowNode, Map<String, Object> varMap) {
        return calculateRoute(flowNode, varMap, true);
    }

    private List<FlowNode> calculateRoute(FlowNode flowNode, Map<String, Object> varMap,
                                          boolean includeRoutingNodes) {
        List<FlowNode> routeNodes = new ArrayList<>();
        calculateRoute(flowNode, varMap, routeNodes, includeRoutingNodes);
        return routeNodes;
    }

    private void calculateRoute(FlowNode flowNode, Map<String, Object> varMap,
                                List<FlowNode> routeNodes, boolean includeRoutingNodes) {
        if (flowNode == null) {
            return;
        }
        Integer type = flowNode.getType();
        // 节点类型为开始节点、结束节点、抄送节点、审批节点时，加入路径
        if (Objects.equals(type, FlowNodeEnum.Type.START.getCode())
                || Objects.equals(type, FlowNodeEnum.Type.END.getCode())
                || Objects.equals(type, FlowNodeEnum.Type.COPY.getCode())
                || Objects.equals(type, FlowNodeEnum.Type.APPROVE.getCode())) {
            routeNodes.add(flowNode);
        }
        // 条件分支
        if (Objects.equals(type, FlowNodeEnum.Type.CONDITION_BRANCH.getCode())) {
            if (includeRoutingNodes) {
                routeNodes.add(flowNode);
            }
            FlowNode conditionNode = branchSelector.selectExclusiveBranch(flowNode, varMap);
            if (conditionNode != null) {
                if (includeRoutingNodes) {
                    routeNodes.add(conditionNode);
                }
                calculateRoute(conditionNode.getChildNode(), varMap, routeNodes, includeRoutingNodes);
            }
        }
        // 并行分支
        if (Objects.equals(type, FlowNodeEnum.Type.PARALLEL_BRANCH.getCode())) {
            if (includeRoutingNodes) {
                routeNodes.add(flowNode);
            }
            List<FlowNode> conditionNodes = branchSelector.selectParallelBranches(flowNode, varMap);
            for (FlowNode conditionNode : conditionNodes) {
                if (includeRoutingNodes) {
                    routeNodes.add(conditionNode);
                }
                FlowNode childNode = conditionNode.getChildNode();
                calculateRoute(childNode, varMap, routeNodes, includeRoutingNodes);
            }
        }
        calculateRoute(flowNode.getChildNode(), varMap, routeNodes, includeRoutingNodes);
    }
}
