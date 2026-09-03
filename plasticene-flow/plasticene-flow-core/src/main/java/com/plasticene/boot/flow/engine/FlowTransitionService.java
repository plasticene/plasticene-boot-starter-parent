package com.plasticene.boot.flow.engine;

import com.plasticene.boot.flow.core.FlowException;
import com.plasticene.boot.flow.core.model.FlowActor;
import com.plasticene.boot.flow.core.model.FlowBranch;
import com.plasticene.boot.flow.core.model.FlowDefinition;
import com.plasticene.boot.flow.core.model.FlowEnums;
import com.plasticene.boot.flow.core.model.FlowEvent;
import com.plasticene.boot.flow.core.model.FlowInstance;
import com.plasticene.boot.flow.core.model.FlowNode;
import com.plasticene.boot.flow.core.model.FlowTask;
import com.plasticene.boot.flow.core.spi.FlowAssigneeProvider;
import com.plasticene.boot.flow.core.spi.FlowRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 驱动流程节点流转与审批任务创建的服务。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public class FlowTransitionService {

    private final FlowRepository repository;
    private final FlowAssigneeProvider assigneeProvider;
    private final Clock clock;
    private final FlowConditionEvaluator conditionEvaluator;

    public FlowTransitionService(FlowRepository repository, FlowAssigneeProvider assigneeProvider, Clock clock) {
        this(repository, assigneeProvider, clock, new FlowConditionEvaluator());
    }

    public FlowTransitionService(FlowRepository repository, FlowAssigneeProvider assigneeProvider, Clock clock,
                                 FlowConditionEvaluator conditionEvaluator) {
        this.repository = repository;
        this.assigneeProvider = assigneeProvider;
        this.clock = clock;
        this.conditionEvaluator = conditionEvaluator;
    }

    public FlowInstance enter(FlowInstance instance, FlowDefinition definition, FlowNode node, FlowActor actor) {
        if (node == null) {
            throw new FlowException("FLOW_MODEL_INVALID", "Flow ended without an END node");
        }
        FlowInstance current = repository.saveInstance(instance.atNode(node.key()));
        return switch (node.type()) {
            case START, COPY -> enter(current, definition, node.childNode(), actor);
            case APPROVAL -> createApprovalTasks(current, definition, node, actor);
            case END -> complete(current, node);
            case CONDITION -> enterCondition(current, definition, node, actor);
        };
    }

    public FlowNode findNode(FlowNode root, String key) {
        if (root == null) {
            return null;
        }
        if (root.key().equals(key)) {
            return root;
        }
        FlowNode found = findNode(root.childNode(), key);
        if (found != null) {
            return found;
        }
        for (FlowBranch branch : root.branches()) {
            found = findNode(branch.childNode(), key);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private FlowInstance enterCondition(FlowInstance instance, FlowDefinition definition, FlowNode node,
                                        FlowActor actor) {
        FlowBranch branch = conditionEvaluator.select(node.branches(), instance.variables());
        return enter(instance, definition, branch.childNode(), actor);
    }

    public FlowInstance advance(FlowInstance instance, FlowDefinition definition, String completedNodeKey,
                                FlowActor actor) {
        FlowNode completedNode = findNode(definition.rootNode(), completedNodeKey);
        if (completedNode == null) {
            throw new FlowException("FLOW_NODE_NOT_FOUND", "Node not found: " + completedNodeKey);
        }
        return enter(instance, definition, completedNode.childNode(), actor);
    }

    private FlowInstance createApprovalTasks(FlowInstance instance, FlowDefinition definition, FlowNode node,
                                             FlowActor actor) {
        List<String> assignees = assigneeProvider.resolveAssignees(actor, definition, node);
        if (assignees == null || assignees.isEmpty()) {
            throw new FlowException("FLOW_ASSIGNEE_EMPTY", "No assignee resolved for node: " + node.key());
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (node.approvalMode() == FlowEnums.ApprovalMode.ORDER) {
            repository.saveTask(new FlowTask(null, instance.tenantId(), instance.id(), node.key(), node.name(),
                    assignees.getFirst(), assignees, 0, node.approvalMode(), FlowEnums.TaskStatus.RUNNING,
                    node.requireComment(), null, now, null));
            return instance;
        }
        for (String assignee : assignees.stream().distinct().toList()) {
            repository.saveTask(new FlowTask(null, instance.tenantId(), instance.id(), node.key(), node.name(),
                    assignee, assignees, 0, node.approvalMode(), FlowEnums.TaskStatus.RUNNING,
                    node.requireComment(), null, now, null));
        }
        return instance;
    }

    private FlowInstance complete(FlowInstance instance, FlowNode node) {
        FlowInstance completed = repository.saveInstance(instance.completed(
                FlowEnums.InstanceStatus.APPROVED, node.key(), LocalDateTime.now(clock)));
        repository.appendEvent(new FlowEvent(UUID.randomUUID().toString(), completed.tenantId(),
                "FLOW_INSTANCE_APPROVED", completed.id(), Map.of("businessKey", completed.businessKey()),
                LocalDateTime.now(clock)));
        return completed;
    }

}
