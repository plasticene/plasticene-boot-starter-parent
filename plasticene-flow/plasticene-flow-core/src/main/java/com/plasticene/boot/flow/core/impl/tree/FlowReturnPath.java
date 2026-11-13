package com.plasticene.boot.flow.core.impl.tree;

import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTask;
import com.plasticene.boot.flow.core.model.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.model.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.model.FlowNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 按模型拓扑计算退回目标、后继影响范围及并行汇合需要撤销的完成贡献。
 * @author ZFJ
 * @since 2026-11-02
 */
public final class FlowReturnPath {
    /** 工具类不允许实例化。 */
    private FlowReturnPath() {
    }

    /**
     * 判断任务记录是否仍有效；已完成的任务也可能有效，不以任务状态作为判断依据。
     *
     * @param task 待判断的任务记录
     * @return 未被退回操作标记失效时返回 true
     */
    public static boolean isActive(FlowTask task) {
        return task.getReturnTaskId() == null;
    }

    /**
     * 按模型遍历顺序获取当前节点可退回的人工审批节点。
     * 候选节点须能沿模型路径到达当前节点，且存在仍有效的人工审批通过记录、没有仍有效且处理中的任务。
     * 此处结合模型可达性与实际任务记录筛选，不将所有模型前置节点直接视为可退回目标。
     *
     * @param root 模型根节点
     * @param currentKey 当前审批节点 key，本节点不能作为退回目标
     * @param tasks 实例下的全部任务记录，可包含已失效记录
     * @return 满足退回条件的模型节点列表
     */
    public static List<FlowNode> targets(FlowNode root, String currentKey, List<FlowTask> tasks) {
        // 分支末端的可达性计算依赖父链，先为整棵模型补齐父节点引用。
        FlowParser.makeParentNode(root);
        List<FlowNode> nodes = new ArrayList<>();
        collect(root, nodes);
        return nodes.stream()
                .filter(node -> Objects.equals(node.getType(), FlowNodeEnum.Type.APPROVE.getCode()))
                .filter(node -> Objects.equals(node.getApproveType(), FlowNodeEnum.ApproveType.MANUAL.getCode()))
                .filter(node -> !Objects.equals(node.getKey(), currentKey))
                // 从候选节点向后可达当前节点，才能作为当前节点的退回目标。
                .filter(node -> reachable(node).contains(currentKey))
                // 仅认可未作废的人工审批通过记录，排除自动通过和旧轮次的审批结果。
                .filter(node -> tasks.stream().filter(FlowReturnPath::isActive)
                        .filter(task -> Objects.equals(task.getNodeKey(), node.getKey()))
                        .anyMatch(task -> Objects.equals(task.getStatus(), FlowTaskStatusEnum.APPROVE.getCode())
                                && Objects.equals(task.getApproveType(), FlowNodeEnum.ApproveType.MANUAL.getCode())))
                // 多人审批尚未完成的节点不能作为目标，避免与该节点现有待办重叠。
                .filter(node -> tasks.stream().filter(FlowReturnPath::isActive)
                        .noneMatch(task -> Objects.equals(task.getNodeKey(), node.getKey())
                                && Objects.equals(task.getStatus(), FlowTaskStatusEnum.RUNNING.getCode())))
                .toList();
    }

    /**
     * 收集从指定节点出发沿模型执行方向可达的节点 key，包含起点本身。
     * 分支末端会沿父链寻找网关出口；调用前须已建立模型的父节点引用。
     *
     * @param node 遍历起点，为 null 时返回空集合
     * @return 按首次访问顺序去重的节点 key 集合
     */
    public static Set<String> reachable(FlowNode node) {
        Set<String> keys = new LinkedHashSet<>();
        visit(node, keys);
        return keys;
    }

    /**
     * 获取退回目标所在的各层并行网关，并推断目标分支是否已贡献汇合计数。
     * 调用前须已建立目标节点和当前节点的父链；此方法只提供判断结果，不修改网关计数。
     *
     * @param target 退回目标节点
     * @param current 当前执行节点
     * @return 按由内到外顺序记录的并行网关 key 与判断结果；true 表示当前节点已不在目标分支内，
     *         退回时需撤销该分支的汇合贡献，false 表示当前节点仍在目标分支内
     */
    public static Map<String, Boolean> enclosingGateways(FlowNode target, FlowNode current) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        // 沿目标父链逐层寻找“分支节点 -> 并行网关”，兼顾嵌套并行分支。
        for (FlowNode ancestor = target.getParentNode(); ancestor != null; ancestor = ancestor.getParentNode()) {
            FlowNode gateway = ancestor.getParentNode();
            if (Objects.equals(ancestor.getType(), FlowNodeEnum.Type.CONDITION_NODE.getCode())
                    && gateway != null && Objects.equals(gateway.getType(), FlowNodeEnum.Type.PARALLEL_BRANCH.getCode())) {
                boolean inBranch = false;
                // 当前节点的父链包含该分支时，说明执行仍在分支内部，尚未越过该层汇合。
                for (FlowNode cursor = current; cursor != null; cursor = cursor.getParentNode()) {
                    if (Objects.equals(cursor.getKey(), ancestor.getKey())) {
                        inBranch = true;
                        break;
                    }
                }
                result.put(gateway.getKey(), !inBranch);
            }
        }
        return result;
    }

    /**
     * 按“当前节点、各条件分支、直接后继”的顺序收集模型中的所有节点。
     * 仅遍历模型树结构，不沿父链寻找执行出口；用于枚举候选节点，与可达路径计算分开处理。
     *
     * @param node 当前模型节点，为 null 时结束本次递归
     * @param nodes 按遍历顺序追加节点的结果列表
     */
    private static void collect(FlowNode node, List<FlowNode> nodes) {
        if (node == null) {
            return;
        }
        nodes.add(node);
        if (node.getConditionNodes() != null) {
            node.getConditionNodes().forEach(branch -> collect(branch, nodes));
        }
        collect(node.getChildNode(), nodes);
    }

    /**
     * 沿模型执行方向递归访问节点，并在分支末端衔接网关之后的路径。
     * 网关会遍历其所有条件分支，此处计算拓扑可达范围，不求值运行时分支条件。
     *
     * @param node 当前访问节点
     * @param keys 已访问节点及结果集合，通过节点 key 去重以避免重复遍历汇合路径
     */
    private static void visit(FlowNode node, Set<String> keys) {
        // 汇合后的节点可能被多条分支访问，已收集过则终止该次递归。
        if (node == null || !keys.add(node.getKey())) {
            return;
        }
        if (node.getConditionNodes() != null) {
            node.getConditionNodes().forEach(branch -> visit(branch, keys));
        }
        if (node.getChildNode() != null) {
            // 存在直接后继时优先向后遍历，后继末端会负责继续寻找外层出口。
            visit(node.getChildNode(), keys);
            return;
        }
        // 没有直接后继时沿父链寻找最近的网关出口；内层没有出口则继续寻找外层。
        // 只进入网关的后继，不重新访问网关及其条件分支，避免误入兄弟分支。
        for (FlowNode cursor = node; cursor != null; cursor = cursor.getParentNode()) {
            if (Objects.equals(cursor.getType(), FlowNodeEnum.Type.CONDITION_NODE.getCode())) {
                FlowNode gateway = cursor.getParentNode();
                if (gateway != null && gateway.getChildNode() != null) {
                    visit(gateway.getChildNode(), keys);
                    return;
                }
            }
        }
    }
}
