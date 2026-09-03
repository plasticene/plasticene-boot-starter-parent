package com.plasticene.boot.flow.engine;

import com.plasticene.boot.flow.core.FlowException;
import com.plasticene.boot.flow.core.FlowEngine;
import com.plasticene.boot.flow.core.command.FlowCommands;
import com.plasticene.boot.flow.core.command.FlowQueries;
import com.plasticene.boot.flow.core.command.FlowResults;
import com.plasticene.boot.flow.core.model.FlowActor;
import com.plasticene.boot.flow.core.model.FlowDefinition;
import com.plasticene.boot.flow.core.model.FlowEnums;
import com.plasticene.boot.flow.core.model.FlowEvent;
import com.plasticene.boot.flow.core.model.FlowInstance;
import com.plasticene.boot.flow.core.model.FlowModel;
import com.plasticene.boot.flow.core.model.FlowNode;
import com.plasticene.boot.flow.core.model.FlowTask;
import com.plasticene.boot.flow.core.model.FormInstance;
import com.plasticene.boot.flow.core.model.FormDefinition;
import com.plasticene.boot.flow.core.spi.FlowRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 嵌入式审批流引擎的默认实现。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public class DefaultFlowEngine implements FlowEngine {

    private static final String SAVE_MODEL = "SAVE_MODEL";
    private static final String SAVE_FORM_DEFINITION = "SAVE_FORM_DEFINITION";
    private static final String PUBLISH_MODEL = "PUBLISH_MODEL";
    private static final String START_FLOW = "START_FLOW";
    private static final String APPROVE_TASK = "APPROVE_TASK";
    private static final String REJECT_TASK = "REJECT_TASK";
    private static final String RETURN_TASK = "RETURN_TASK";
    private static final String TRANSFER_TASK = "TRANSFER_TASK";
    private static final String WITHDRAW_FLOW = "WITHDRAW_FLOW";
    private static final String CANCEL_FLOW = "CANCEL_FLOW";

    private final FlowRepository repository;
    private final FlowModelValidator modelValidator;
    private final FlowTransitionService transitionService;
    private final Clock clock;

    public DefaultFlowEngine(FlowRepository repository, FlowModelValidator modelValidator,
                             FlowTransitionService transitionService, Clock clock) {
        this.repository = repository;
        this.modelValidator = modelValidator;
        this.transitionService = transitionService;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowResults.ModelResult saveModel(FlowCommands.SaveModel command) {
        Optional<Long> replay = replay(command.actor().tenantId(), command.requestId(), SAVE_MODEL);
        if (replay.isPresent()) {
            return new FlowResults.ModelResult(requireModel(command.actor().tenantId(), replay.get(), false), true);
        }
        claim(command.actor().tenantId(), command.requestId(), SAVE_MODEL);
        FlowModel existing = command.modelId() == null ? null
                : requireModel(command.actor().tenantId(), command.modelId(), true);
        FlowModel model = new FlowModel(command.modelId(), command.actor().tenantId(), command.code(),
                command.name(), existing == null ? FlowEnums.ModelStatus.DRAFT : existing.status(),
                command.formDefinitionId(), command.rootNode(), existing == null ? null : existing.activeDefinitionId(),
                existing == null ? 0 : existing.activeVersion(), existing == null ? null : existing.publishedAt());
        FlowModel saved = repository.saveModel(model);
        repository.completeCommand(command.actor().tenantId(), command.requestId(), SAVE_MODEL, saved.id());
        return new FlowResults.ModelResult(saved, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowResults.FormDefinitionResult saveFormDefinition(FlowCommands.SaveFormDefinition command) {
        Optional<Long> replay = replay(command.actor().tenantId(), command.requestId(), SAVE_FORM_DEFINITION);
        if (replay.isPresent()) {
            FormDefinition saved = repository.findFormDefinition(command.actor().tenantId(), replay.get())
                    .orElseThrow(() -> new FlowException("FLOW_FORM_NOT_FOUND", "Form definition not found"));
            return new FlowResults.FormDefinitionResult(saved, true);
        }
        claim(command.actor().tenantId(), command.requestId(), SAVE_FORM_DEFINITION);
        if (command.formDefinitionId() != null) {
            repository.findFormDefinition(command.actor().tenantId(), command.formDefinitionId())
                    .orElseThrow(() -> new FlowException("FLOW_FORM_NOT_FOUND", "Form definition not found"));
        }
        FormDefinition saved = repository.saveFormDefinition(new FormDefinition(command.formDefinitionId(),
                command.actor().tenantId(), command.code(), command.name(), 1, command.schema()));
        repository.completeCommand(command.actor().tenantId(), command.requestId(), SAVE_FORM_DEFINITION, saved.id());
        return new FlowResults.FormDefinitionResult(saved, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowResults.DefinitionResult publishModel(FlowCommands.PublishModel command) {
        Optional<Long> replay = replay(command.actor().tenantId(), command.requestId(), PUBLISH_MODEL);
        if (replay.isPresent()) {
            return new FlowResults.DefinitionResult(requireDefinition(command.actor().tenantId(), replay.get()), true);
        }
        claim(command.actor().tenantId(), command.requestId(), PUBLISH_MODEL);
        FlowModel model = requireModel(command.actor().tenantId(), command.modelId(), true);
        modelValidator.validate(model.modelNode());
        int version = repository.nextDefinitionVersion(model.tenantId(), model.id());
        LocalDateTime now = LocalDateTime.now(clock);
        FlowDefinition definition = repository.saveDefinition(new FlowDefinition(null, model.tenantId(), model.id(),
                model.code(), model.name(), version, model.formDefinitionId(), model.modelNode(), now));
        repository.saveModel(model.published(definition, now));
        repository.completeCommand(command.actor().tenantId(), command.requestId(), PUBLISH_MODEL, definition.id());
        return new FlowResults.DefinitionResult(definition, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowResults.InstanceResult startFlow(FlowCommands.StartFlow command) {
        Optional<Long> replay = replay(command.actor().tenantId(), command.requestId(), START_FLOW);
        if (replay.isPresent()) {
            return new FlowResults.InstanceResult(requireInstance(command.actor().tenantId(), replay.get(), false), true);
        }
        claim(command.actor().tenantId(), command.requestId(), START_FLOW);
        FlowModel model = repository.findModelByCode(command.actor().tenantId(), command.modelCode())
                .filter(value -> value.status() == FlowEnums.ModelStatus.PUBLISHED)
                .orElseThrow(() -> new FlowException("FLOW_MODEL_NOT_ACTIVE", "Published model not found"));
        FlowDefinition definition = requireDefinition(command.actor().tenantId(), model.activeDefinitionId());
        if (definition.formDefinitionId() != null) {
            repository.findFormDefinition(command.actor().tenantId(), definition.formDefinitionId())
                    .orElseThrow(() -> new FlowException("FLOW_FORM_NOT_FOUND", "Form definition not found"));
        }
        LocalDateTime now = LocalDateTime.now(clock);
        FormInstance form = repository.saveFormInstance(new FormInstance(null, command.actor().tenantId(),
                definition.id(), definition.formDefinitionId(), command.businessKey(), command.formData(), now));
        FlowInstance instance = repository.saveInstance(new FlowInstance(null, command.actor().tenantId(),
                definition.id(), command.actor().operatorId(), command.businessKey(), FlowEnums.InstanceStatus.RUNNING,
                definition.rootNode().key(), form.id(), command.variables(), now, null));
        FlowInstance progressed = transitionService.enter(instance, definition, definition.rootNode(), command.actor());
        repository.appendEvent(new FlowEvent(UUID.randomUUID().toString(), progressed.tenantId(),
                "FLOW_INSTANCE_STARTED", progressed.id(), Map.of("businessKey", progressed.businessKey()), now));
        repository.completeCommand(command.actor().tenantId(), command.requestId(), START_FLOW, progressed.id());
        return new FlowResults.InstanceResult(progressed, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowResults.TaskResult approveTask(FlowCommands.ApproveTask command) {
        Optional<Long> replay = replay(command.actor().tenantId(), command.requestId(), APPROVE_TASK);
        if (replay.isPresent()) {
            FlowTask task = requireTask(command.actor().tenantId(), replay.get());
            return new FlowResults.TaskResult(task,
                    requireInstance(command.actor().tenantId(), task.instanceId(), false), true);
        }
        claim(command.actor().tenantId(), command.requestId(), APPROVE_TASK);
        FlowTask task = requireTask(command.actor().tenantId(), command.taskId());
        FlowInstance instance = requireInstance(command.actor().tenantId(), task.instanceId(), true);
        verifyTask(command.actor().operatorId(), task, command.comment());
        FlowTask completed = task.completed(FlowEnums.TaskStatus.APPROVED, command.comment(), LocalDateTime.now(clock));
        if (!repository.completeTask(completed)) {
            throw new FlowException("FLOW_TASK_ALREADY_COMPLETED", "Task has already been completed");
        }
        FlowDefinition definition = requireDefinition(command.actor().tenantId(), instance.definitionId());
        FlowInstance progressed = progressAfterApproval(command, completed, instance, definition);
        repository.completeCommand(command.actor().tenantId(), command.requestId(), APPROVE_TASK, completed.id());
        return new FlowResults.TaskResult(completed, progressed, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowResults.TaskResult rejectTask(FlowCommands.RejectTask command) {
        Optional<Long> replay = replay(command.actor().tenantId(), command.requestId(), REJECT_TASK);
        if (replay.isPresent()) {
            FlowTask task = requireTask(command.actor().tenantId(), replay.get());
            return new FlowResults.TaskResult(task,
                    requireInstance(command.actor().tenantId(), task.instanceId(), false), true);
        }
        claim(command.actor().tenantId(), command.requestId(), REJECT_TASK);
        FlowTask task = requireTask(command.actor().tenantId(), command.taskId());
        FlowInstance instance = requireInstance(command.actor().tenantId(), task.instanceId(), true);
        verifyTask(command.actor().operatorId(), task, command.comment());
        LocalDateTime now = LocalDateTime.now(clock);
        FlowTask rejected = task.completed(FlowEnums.TaskStatus.REJECTED, command.comment(), now);
        if (!repository.completeTask(rejected)) {
            throw new FlowException("FLOW_TASK_ALREADY_COMPLETED", "Task has already been completed");
        }
        repository.cancelRunningTasks(instance.tenantId(), instance.id(), task.nodeKey());
        FlowInstance ended = repository.saveInstance(instance.completed(
                FlowEnums.InstanceStatus.REJECTED, task.nodeKey(), now));
        repository.appendEvent(new FlowEvent(UUID.randomUUID().toString(), ended.tenantId(),
                "FLOW_INSTANCE_REJECTED", ended.id(), Map.of("taskId", rejected.id()), now));
        repository.completeCommand(command.actor().tenantId(), command.requestId(), REJECT_TASK, rejected.id());
        return new FlowResults.TaskResult(rejected, ended, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowResults.TaskResult returnTask(FlowCommands.ReturnTask command) {
        Optional<Long> replay = replay(command.actor().tenantId(), command.requestId(), RETURN_TASK);
        if (replay.isPresent()) {
            FlowTask task = requireTask(command.actor().tenantId(), replay.get());
            return new FlowResults.TaskResult(task,
                    requireInstance(command.actor().tenantId(), task.instanceId(), false), true);
        }
        claim(command.actor().tenantId(), command.requestId(), RETURN_TASK);
        FlowTask task = requireTask(command.actor().tenantId(), command.taskId());
        FlowInstance instance = requireInstance(command.actor().tenantId(), task.instanceId(), true);
        verifyTask(command.actor().operatorId(), task, command.comment());
        FlowDefinition definition = requireDefinition(command.actor().tenantId(), instance.definitionId());
        FlowNode target = transitionService.findNode(definition.rootNode(), command.targetNodeKey());
        if (target == null || target.type() != FlowEnums.NodeType.APPROVAL
                || target.key().equals(task.nodeKey())) {
            throw new FlowException("FLOW_RETURN_TARGET_INVALID", "Return target must be another approval node");
        }
        boolean previouslyVisited = repository.findTasksByInstance(instance.tenantId(), instance.id()).stream()
                .anyMatch(history -> history.nodeKey().equals(target.key()));
        if (!previouslyVisited) {
            throw new FlowException("FLOW_RETURN_TARGET_INVALID", "Return target has not been visited");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        FlowTask returned = task.completed(FlowEnums.TaskStatus.RETURNED, command.comment(), now);
        if (!repository.completeTask(returned)) {
            throw new FlowException("FLOW_TASK_ALREADY_COMPLETED", "Task has already been completed");
        }
        repository.cancelRunningTasks(instance.tenantId(), instance.id(), task.nodeKey());
        FlowInstance progressed = transitionService.enter(instance.resumedAt(target.key()), definition, target,
                command.actor());
        appendEvent(progressed, "FLOW_TASK_RETURNED",
                Map.of("taskId", returned.id(), "targetNodeKey", target.key()), now);
        repository.completeCommand(command.actor().tenantId(), command.requestId(), RETURN_TASK, returned.id());
        return new FlowResults.TaskResult(returned, progressed, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowResults.TaskResult transferTask(FlowCommands.TransferTask command) {
        Optional<Long> replay = replay(command.actor().tenantId(), command.requestId(), TRANSFER_TASK);
        if (replay.isPresent()) {
            FlowTask task = requireTask(command.actor().tenantId(), replay.get());
            return new FlowResults.TaskResult(task,
                    requireInstance(command.actor().tenantId(), task.instanceId(), false), true);
        }
        claim(command.actor().tenantId(), command.requestId(), TRANSFER_TASK);
        FlowTask task = requireTask(command.actor().tenantId(), command.taskId());
        FlowInstance instance = requireInstance(command.actor().tenantId(), task.instanceId(), true);
        verifyRunningAssignee(command.actor().operatorId(), task);
        if (task.assigneeId().equals(command.newAssigneeId())) {
            throw new FlowException("FLOW_TRANSFER_TARGET_INVALID", "New assignee must be different");
        }
        FlowTask transferred = task.transferredTo(command.newAssigneeId());
        if (!repository.transferTask(transferred, task.assigneeId())) {
            throw new FlowException("FLOW_TASK_ALREADY_COMPLETED", "Task has already changed");
        }
        appendEvent(instance, "FLOW_TASK_TRANSFERRED", Map.of("taskId", task.id(),
                "from", task.assigneeId(), "to", transferred.assigneeId()), LocalDateTime.now(clock));
        repository.completeCommand(command.actor().tenantId(), command.requestId(), TRANSFER_TASK, transferred.id());
        return new FlowResults.TaskResult(transferred, instance, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowResults.InstanceResult withdrawFlow(FlowCommands.WithdrawFlow command) {
        Optional<Long> replay = replay(command.actor().tenantId(), command.requestId(), WITHDRAW_FLOW);
        if (replay.isPresent()) {
            return new FlowResults.InstanceResult(requireInstance(command.actor().tenantId(), replay.get(), false), true);
        }
        claim(command.actor().tenantId(), command.requestId(), WITHDRAW_FLOW);
        FlowInstance instance = requireInstance(command.actor().tenantId(), command.instanceId(), true);
        verifyRequester(command.actor(), instance);
        boolean processed = repository.findTasksByInstance(instance.tenantId(), instance.id()).stream()
                .anyMatch(task -> task.status() != FlowEnums.TaskStatus.RUNNING
                        && task.status() != FlowEnums.TaskStatus.CANCELLED);
        if (processed) {
            throw new FlowException("FLOW_WITHDRAW_NOT_ALLOWED", "A task has already been processed");
        }
        FlowInstance withdrawn = cancel(instance, "FLOW_INSTANCE_WITHDRAWN", "withdrawn");
        repository.completeCommand(command.actor().tenantId(), command.requestId(), WITHDRAW_FLOW, withdrawn.id());
        return new FlowResults.InstanceResult(withdrawn, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowResults.InstanceResult cancelFlow(FlowCommands.CancelFlow command) {
        Optional<Long> replay = replay(command.actor().tenantId(), command.requestId(), CANCEL_FLOW);
        if (replay.isPresent()) {
            return new FlowResults.InstanceResult(requireInstance(command.actor().tenantId(), replay.get(), false), true);
        }
        claim(command.actor().tenantId(), command.requestId(), CANCEL_FLOW);
        FlowInstance instance = requireInstance(command.actor().tenantId(), command.instanceId(), true);
        verifyRequester(command.actor(), instance);
        FlowInstance cancelled = cancel(instance, "FLOW_INSTANCE_CANCELLED", command.reason());
        repository.completeCommand(command.actor().tenantId(), command.requestId(), CANCEL_FLOW, cancelled.id());
        return new FlowResults.InstanceResult(cancelled, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlowModel> findModels(FlowQueries.ModelList query) {
        return repository.findModels(query.actor().tenantId(), query.status(), query.offset(), query.limit());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlowDefinition> findDefinitions(FlowQueries.DefinitionList query) {
        return repository.findDefinitions(query.actor().tenantId(), query.modelId(), query.offset(), query.limit());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlowInstance> findMyStartedFlows(FlowQueries.MyStartedFlows query) {
        return repository.findInstancesByRequester(query.actor().tenantId(), query.actor().operatorId(),
                query.status(), query.offset(), query.limit());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlowTask> findMyTasks(FlowQueries.MyTasks query) {
        return repository.findTasksByAssignee(query.actor().tenantId(), query.actor().operatorId(),
                query.status(), query.offset(), query.limit());
    }

    @Override
    @Transactional(readOnly = true)
    public FlowResults.InstanceDetails getInstanceDetails(FlowQueries.InstanceDetails query) {
        FlowInstance instance = requireInstance(query.actor().tenantId(), query.instanceId(), false);
        FlowDefinition definition = requireDefinition(query.actor().tenantId(), instance.definitionId());
        FormInstance form = instance.formInstanceId() == null ? null
                : repository.findFormInstance(query.actor().tenantId(), instance.formInstanceId()).orElse(null);
        return new FlowResults.InstanceDetails(instance, definition, form,
                repository.findTasksByInstance(query.actor().tenantId(), instance.id()));
    }

    private FlowInstance cancel(FlowInstance instance, String eventType, String reason) {
        if (instance.status() != FlowEnums.InstanceStatus.RUNNING) {
            throw new FlowException("FLOW_INSTANCE_NOT_RUNNING", "Flow instance is not running");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        repository.cancelRunningTasks(instance.tenantId(), instance.id());
        FlowInstance cancelled = repository.saveInstance(instance.completed(
                FlowEnums.InstanceStatus.CANCELLED, instance.currentNodeKey(), now));
        appendEvent(cancelled, eventType, Map.of("reason", reason), now);
        return cancelled;
    }

    private void verifyRequester(FlowActor actor, FlowInstance instance) {
        if (!instance.requesterId().equals(actor.operatorId())) {
            throw new FlowException("FLOW_INSTANCE_FORBIDDEN", "Only the requester can operate this instance");
        }
    }

    private void appendEvent(FlowInstance instance, String eventType, Map<String, Object> payload,
                             LocalDateTime time) {
        repository.appendEvent(new FlowEvent(UUID.randomUUID().toString(), instance.tenantId(), eventType,
                instance.id(), payload, time));
    }

    private FlowInstance progressAfterApproval(FlowCommands.ApproveTask command, FlowTask task,
                                               FlowInstance instance, FlowDefinition definition) {
        if (task.approvalMode() == FlowEnums.ApprovalMode.ALL
                && !repository.findRunningTasks(instance.tenantId(), instance.id(), task.nodeKey()).isEmpty()) {
            return instance;
        }
        if (task.approvalMode() == FlowEnums.ApprovalMode.ANY) {
            repository.cancelRunningTasks(instance.tenantId(), instance.id(), task.nodeKey());
        }
        if (task.approvalMode() == FlowEnums.ApprovalMode.ORDER
                && task.orderIndex() + 1 < task.approvalOrder().size()) {
            int nextIndex = task.orderIndex() + 1;
            repository.saveTask(new FlowTask(null, task.tenantId(), task.instanceId(), task.nodeKey(), task.nodeName(),
                    task.approvalOrder().get(nextIndex), task.approvalOrder(), nextIndex, task.approvalMode(),
                    FlowEnums.TaskStatus.RUNNING, task.requireComment(), null, LocalDateTime.now(clock), null));
            return instance;
        }
        return transitionService.advance(instance, definition, task.nodeKey(), command.actor());
    }

    private void verifyTask(String operatorId, FlowTask task, String comment) {
        verifyRunningAssignee(operatorId, task);
        if (task.requireComment() && (comment == null || comment.isBlank())) {
            throw new FlowException("FLOW_COMMENT_REQUIRED", "Approval comment is required");
        }
    }

    private void verifyRunningAssignee(String operatorId, FlowTask task) {
        if (task.status() != FlowEnums.TaskStatus.RUNNING) {
            throw new FlowException("FLOW_TASK_ALREADY_COMPLETED", "Task has already been completed");
        }
        if (!task.assigneeId().equals(operatorId)) {
            throw new FlowException("FLOW_TASK_FORBIDDEN", "Only the assignee can operate this task");
        }
    }

    private void claim(String tenantId, String requestId, String type) {
        if (!repository.claimCommand(tenantId, requestId, type)) {
            throw new FlowException("FLOW_REQUEST_IN_PROGRESS", "Request is already being processed");
        }
    }

    private Optional<Long> replay(String tenantId, String requestId, String type) {
        return repository.findCommandResult(tenantId, requestId, type);
    }

    private FlowModel requireModel(String tenantId, Long id, boolean forUpdate) {
        return repository.findModel(tenantId, id, forUpdate)
                .orElseThrow(() -> new FlowException("FLOW_MODEL_NOT_FOUND", "Flow model not found: " + id));
    }

    private FlowDefinition requireDefinition(String tenantId, Long id) {
        return repository.findDefinition(tenantId, id)
                .orElseThrow(() -> new FlowException("FLOW_DEFINITION_NOT_FOUND", "Flow definition not found: " + id));
    }

    private FlowInstance requireInstance(String tenantId, Long id, boolean forUpdate) {
        return repository.findInstance(tenantId, id, forUpdate)
                .orElseThrow(() -> new FlowException("FLOW_INSTANCE_NOT_FOUND", "Flow instance not found: " + id));
    }

    private FlowTask requireTask(String tenantId, Long id) {
        return repository.findTask(tenantId, id)
                .orElseThrow(() -> new FlowException("FLOW_TASK_NOT_FOUND", "Flow task not found: " + id));
    }
}
