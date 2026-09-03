package com.plasticene.boot.flow.engine;

import com.plasticene.boot.flow.core.command.FlowCommands;
import com.plasticene.boot.flow.core.command.FlowQueries;
import com.plasticene.boot.flow.core.command.FlowResults;
import com.plasticene.boot.flow.core.model.FlowActor;
import com.plasticene.boot.flow.core.model.FlowBranch;
import com.plasticene.boot.flow.core.model.FlowCondition;
import com.plasticene.boot.flow.core.model.FlowDefinition;
import com.plasticene.boot.flow.core.model.FlowEnums;
import com.plasticene.boot.flow.core.model.FlowEvent;
import com.plasticene.boot.flow.core.model.FlowInstance;
import com.plasticene.boot.flow.core.model.FlowModel;
import com.plasticene.boot.flow.core.model.FlowNode;
import com.plasticene.boot.flow.core.model.FlowOutboxEvent;
import com.plasticene.boot.flow.core.model.FlowTask;
import com.plasticene.boot.flow.core.model.FormDefinition;
import com.plasticene.boot.flow.core.model.FormInstance;
import com.plasticene.boot.flow.core.spi.FlowRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 默认审批流引擎单元测试。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
class DefaultFlowEngineTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-03T00:00:00Z"), ZoneOffset.UTC);
    private static final FlowActor OWNER = new FlowActor("tenant-a", "owner", Set.of());

    @Test
    void allApprovalWaitsForEveryAssigneeAndReplaysTheSameRequest() {
        TestContext context = context(FlowEnums.ApprovalMode.ALL, List.of("alice", "bob"));

        FlowResults.InstanceResult started = context.start("start-1");
        List<FlowTask> tasks = context.repository.runningTasks(started.instance().id());
        assertThat(tasks).extracting(FlowTask::assigneeId).containsExactly("alice", "bob");

        FlowResults.TaskResult first = context.engine.approveTask(new FlowCommands.ApproveTask(
                "approve-1", actor("alice"), tasks.getFirst().id(), "ok"));
        assertThat(first.instance().status()).isEqualTo(FlowEnums.InstanceStatus.RUNNING);

        FlowResults.TaskResult second = context.engine.approveTask(new FlowCommands.ApproveTask(
                "approve-2", actor("bob"), tasks.getLast().id(), "ok"));
        assertThat(second.instance().status()).isEqualTo(FlowEnums.InstanceStatus.APPROVED);

        FlowResults.TaskResult replay = context.engine.approveTask(new FlowCommands.ApproveTask(
                "approve-2", actor("bob"), tasks.getLast().id(), "ok"));
        assertThat(replay.replayed()).isTrue();
        assertThat(context.repository.events).extracting(FlowEvent::eventType)
                .contains("FLOW_INSTANCE_STARTED", "FLOW_INSTANCE_APPROVED");
    }

    @Test
    void anyApprovalCancelsTheOtherTasks() {
        TestContext context = context(FlowEnums.ApprovalMode.ANY, List.of("alice", "bob"));
        FlowResults.InstanceResult started = context.start("start-any");
        FlowTask aliceTask = context.repository.runningTasks(started.instance().id()).getFirst();

        FlowResults.TaskResult result = context.engine.approveTask(new FlowCommands.ApproveTask(
                "approve-any", actor("alice"), aliceTask.id(), null));

        assertThat(result.instance().status()).isEqualTo(FlowEnums.InstanceStatus.APPROVED);
        assertThat(context.repository.runningTasks(started.instance().id())).isEmpty();
    }

    @Test
    void orderedApprovalCreatesOnlyTheNextAssigneeTask() {
        TestContext context = context(FlowEnums.ApprovalMode.ORDER, List.of("alice", "bob"));
        FlowResults.InstanceResult started = context.start("start-order");
        FlowTask aliceTask = context.repository.runningTasks(started.instance().id()).getFirst();

        FlowResults.TaskResult first = context.engine.approveTask(new FlowCommands.ApproveTask(
                "approve-order-1", actor("alice"), aliceTask.id(), null));

        assertThat(first.instance().status()).isEqualTo(FlowEnums.InstanceStatus.RUNNING);
        List<FlowTask> remaining = context.repository.runningTasks(started.instance().id());
        assertThat(remaining).singleElement().extracting(FlowTask::assigneeId).isEqualTo("bob");
    }

    @Test
    void editingAFormKeepsTheExistingFormIdentity() {
        InMemoryFlowRepository repository = new InMemoryFlowRepository();
        DefaultFlowEngine engine = new DefaultFlowEngine(repository, new FlowModelValidator(),
                new FlowTransitionService(repository,
                        (actor, definition, node) -> node.configuredAssignees(), CLOCK), CLOCK);
        FormDefinition first = engine.saveFormDefinition(new FlowCommands.SaveFormDefinition(
                "form-v1", OWNER, null, "leave-form", "Leave form", Map.of("required", false)))
                .formDefinition();

        FormDefinition second = engine.saveFormDefinition(new FlowCommands.SaveFormDefinition(
                "form-v2", OWNER, first.id(), "leave-form", "Leave form", Map.of("required", true)))
                .formDefinition();

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.version()).isEqualTo(1);
        assertThat(repository.findFormDefinition("tenant-a", first.id())).contains(second);
    }

    @Test
    void transfersAndQueriesATaskAndInstanceDetails() {
        TestContext context = context(FlowEnums.ApprovalMode.ANY, List.of("alice"));
        FlowResults.InstanceResult started = context.start("start-query");
        FlowTask task = context.repository.runningTasks(started.instance().id()).getFirst();

        context.engine.transferTask(new FlowCommands.TransferTask(
                "transfer-1", actor("alice"), task.id(), "charlie", "handover"));

        assertThat(context.engine.findMyTasks(new FlowQueries.MyTasks(
                actor("charlie"), FlowEnums.TaskStatus.RUNNING, 0, 20)))
                .singleElement().extracting(FlowTask::assigneeId).isEqualTo("charlie");
        assertThat(context.engine.findMyStartedFlows(new FlowQueries.MyStartedFlows(
                OWNER, FlowEnums.InstanceStatus.RUNNING, 0, 20))).contains(started.instance());
        assertThat(context.engine.getInstanceDetails(new FlowQueries.InstanceDetails(
                OWNER, started.instance().id())).tasks()).hasSize(1);
    }

    @Test
    void withdrawsAnUnprocessedFlow() {
        TestContext context = context(FlowEnums.ApprovalMode.ANY, List.of("alice"));
        FlowInstance started = context.start("start-withdraw").instance();

        FlowResults.InstanceResult withdrawn = context.engine.withdrawFlow(
                new FlowCommands.WithdrawFlow("withdraw-1", OWNER, started.id()));

        assertThat(withdrawn.instance().status()).isEqualTo(FlowEnums.InstanceStatus.CANCELLED);
        assertThat(context.repository.runningTasks(started.id())).isEmpty();
    }

    @Test
    void returnsATaskToAPreviouslyVisitedApprovalNode() {
        FlowNode end = new FlowNode("end", "End", FlowEnums.NodeType.END, null, false, List.of(), null);
        FlowNode second = new FlowNode("second", "Second", FlowEnums.NodeType.APPROVAL,
                FlowEnums.ApprovalMode.ANY, false, List.of("bob"), end);
        FlowNode first = new FlowNode("first", "First", FlowEnums.NodeType.APPROVAL,
                FlowEnums.ApprovalMode.ANY, false, List.of("alice"), second);
        TestContext context = context(new FlowNode("start", "Start", FlowEnums.NodeType.START,
                null, false, List.of(), first));
        FlowInstance instance = context.start("start-return").instance();
        FlowTask firstTask = context.repository.runningTasks(instance.id()).getFirst();
        context.engine.approveTask(new FlowCommands.ApproveTask("approve-first", actor("alice"),
                firstTask.id(), null));
        FlowTask secondTask = context.repository.runningTasks(instance.id()).getFirst();

        context.engine.returnTask(new FlowCommands.ReturnTask(
                "return-1", actor("bob"), secondTask.id(), "first", "please revise"));

        assertThat(context.repository.runningTasks(instance.id()))
                .singleElement().satisfies(task -> {
                    assertThat(task.nodeKey()).isEqualTo("first");
                    assertThat(task.assigneeId()).isEqualTo("alice");
                });
    }

    @Test
    void selectsAConditionBranchFromVariables() {
        FlowNode highEnd = new FlowNode("high-end", "High end", FlowEnums.NodeType.END,
                null, false, List.of(), null);
        FlowNode highApproval = new FlowNode("high", "High approval", FlowEnums.NodeType.APPROVAL,
                FlowEnums.ApprovalMode.ANY, false, List.of("manager"), highEnd);
        FlowNode lowEnd = new FlowNode("low-end", "Low end", FlowEnums.NodeType.END,
                null, false, List.of(), null);
        FlowNode lowApproval = new FlowNode("low", "Low approval", FlowEnums.NodeType.APPROVAL,
                FlowEnums.ApprovalMode.ANY, false, List.of("supervisor"), lowEnd);
        FlowCondition highValue = new FlowCondition(FlowEnums.LogicalOperator.OR, List.of(
                new FlowCondition.Group(FlowEnums.LogicalOperator.AND, List.of(
                        new FlowCondition.Rule("amount", FlowEnums.ComparisonOperator.GT, 100)))));
        FlowNode condition = new FlowNode("amount-condition", "Amount condition", FlowEnums.NodeType.CONDITION,
                null, false, List.of(), null, List.of(
                new FlowBranch("high", highValue, false, highApproval),
                new FlowBranch("default", null, true, lowApproval)));
        TestContext context = context(new FlowNode("start", "Start", FlowEnums.NodeType.START,
                null, false, List.of(), condition));

        FlowInstance instance = context.engine.startFlow(new FlowCommands.StartFlow(
                "start-condition", OWNER, "leave", "biz-condition", Map.of("amount", 120), Map.of())).instance();

        assertThat(context.repository.runningTasks(instance.id()))
                .singleElement().extracting(FlowTask::assigneeId).isEqualTo("manager");
    }

    private static TestContext context(FlowEnums.ApprovalMode mode, List<String> assignees) {
        FlowNode end = new FlowNode("end", "End", FlowEnums.NodeType.END, null, false, List.of(), null);
        FlowNode approve = new FlowNode("approve", "Approve", FlowEnums.NodeType.APPROVAL,
                mode, false, assignees, end);
        FlowNode start = new FlowNode("start", "Start", FlowEnums.NodeType.START,
                null, false, List.of(), approve);
        return context(start);
    }

    private static TestContext context(FlowNode start) {
        InMemoryFlowRepository repository = new InMemoryFlowRepository();
        FlowTransitionService transitionService = new FlowTransitionService(repository,
                (actor, definition, node) -> node.configuredAssignees(), CLOCK);
        DefaultFlowEngine engine = new DefaultFlowEngine(repository, new FlowModelValidator(), transitionService, CLOCK);
        FormDefinition form = engine.saveFormDefinition(new FlowCommands.SaveFormDefinition(
                "form-1", OWNER, null, "leave-form", "Leave form", Map.of("type", "object")))
                .formDefinition();
        FlowModel model = engine.saveModel(new FlowCommands.SaveModel(
                "model-1", OWNER, null, "leave", "Leave", form.id(), start)).model();
        engine.publishModel(new FlowCommands.PublishModel("publish-1", OWNER, model.id()));
        return new TestContext(engine, repository);
    }

    private static FlowActor actor(String operatorId) {
        return new FlowActor("tenant-a", operatorId, Set.of());
    }

    private record TestContext(DefaultFlowEngine engine, InMemoryFlowRepository repository) {

        FlowResults.InstanceResult start(String requestId) {
            return engine.startFlow(new FlowCommands.StartFlow(requestId, OWNER, "leave", "biz-1",
                    Map.of(), Map.of("days", 2)));
        }
    }

    private static final class InMemoryFlowRepository implements FlowRepository {

        private final AtomicLong ids = new AtomicLong();
        private final Map<Long, FlowModel> models = new LinkedHashMap<>();
        private final Map<Long, FlowDefinition> definitions = new LinkedHashMap<>();
        private final Map<Long, FormDefinition> forms = new LinkedHashMap<>();
        private final Map<Long, FormInstance> formInstances = new LinkedHashMap<>();
        private final Map<Long, FlowInstance> instances = new LinkedHashMap<>();
        private final Map<Long, FlowTask> tasks = new LinkedHashMap<>();
        private final Map<String, Long> commandResults = new LinkedHashMap<>();
        private final Set<String> claimedCommands = new java.util.HashSet<>();
        private final List<FlowEvent> events = new ArrayList<>();

        @Override
        public FlowModel saveModel(FlowModel model) {
            FlowModel saved = model.id() == null ? model.withId(ids.incrementAndGet()) : model;
            models.put(saved.id(), saved);
            return saved;
        }

        @Override
        public Optional<FlowModel> findModel(String tenantId, Long modelId, boolean forUpdate) {
            return Optional.ofNullable(models.get(modelId)).filter(value -> value.tenantId().equals(tenantId));
        }

        @Override
        public Optional<FlowModel> findModelByCode(String tenantId, String code) {
            return models.values().stream()
                    .filter(value -> value.tenantId().equals(tenantId) && value.code().equals(code)).findFirst();
        }

        @Override
        public List<FlowModel> findModels(String tenantId, FlowEnums.ModelStatus status, int offset, int limit) {
            return models.values().stream().filter(value -> value.tenantId().equals(tenantId))
                    .filter(value -> status == null || value.status() == status).skip(offset).limit(limit).toList();
        }

        @Override
        public int nextDefinitionVersion(String tenantId, Long modelId) {
            return definitions.values().stream()
                    .filter(value -> value.tenantId().equals(tenantId) && value.modelId().equals(modelId))
                    .mapToInt(FlowDefinition::version).max().orElse(0) + 1;
        }

        @Override
        public FlowDefinition saveDefinition(FlowDefinition definition) {
            FlowDefinition saved = definition.withId(ids.incrementAndGet());
            definitions.put(saved.id(), saved);
            return saved;
        }

        @Override
        public Optional<FlowDefinition> findDefinition(String tenantId, Long definitionId) {
            return Optional.ofNullable(definitions.get(definitionId))
                    .filter(value -> value.tenantId().equals(tenantId));
        }

        @Override
        public List<FlowDefinition> findDefinitions(String tenantId, Long modelId, int offset, int limit) {
            return definitions.values().stream().filter(value -> value.tenantId().equals(tenantId)
                    && value.modelId().equals(modelId)).skip(offset).limit(limit).toList();
        }

        @Override
        public FormDefinition saveFormDefinition(FormDefinition formDefinition) {
            FormDefinition saved = formDefinition.id() == null
                    ? formDefinition.withId(ids.incrementAndGet()) : formDefinition;
            forms.put(saved.id(), saved);
            return saved;
        }

        @Override
        public Optional<FormDefinition> findFormDefinition(String tenantId, Long formDefinitionId) {
            return Optional.ofNullable(forms.get(formDefinitionId)).filter(value -> value.tenantId().equals(tenantId));
        }

        @Override
        public FormInstance saveFormInstance(FormInstance formInstance) {
            FormInstance saved = formInstance.withId(ids.incrementAndGet());
            formInstances.put(saved.id(), saved);
            return saved;
        }

        @Override
        public Optional<FormInstance> findFormInstance(String tenantId, Long formInstanceId) {
            return Optional.ofNullable(formInstances.get(formInstanceId))
                    .filter(value -> value.tenantId().equals(tenantId));
        }

        @Override
        public FlowInstance saveInstance(FlowInstance instance) {
            FlowInstance saved = instance.id() == null ? instance.withId(ids.incrementAndGet()) : instance;
            instances.put(saved.id(), saved);
            return saved;
        }

        @Override
        public Optional<FlowInstance> findInstance(String tenantId, Long instanceId, boolean forUpdate) {
            return Optional.ofNullable(instances.get(instanceId)).filter(value -> value.tenantId().equals(tenantId));
        }

        @Override
        public List<FlowInstance> findInstancesByRequester(String tenantId, String requesterId,
                                                           FlowEnums.InstanceStatus status, int offset, int limit) {
            return instances.values().stream().filter(value -> value.tenantId().equals(tenantId)
                    && value.requesterId().equals(requesterId))
                    .filter(value -> status == null || value.status() == status).skip(offset).limit(limit).toList();
        }

        @Override
        public FlowTask saveTask(FlowTask task) {
            FlowTask saved = task.id() == null ? task.withId(ids.incrementAndGet()) : task;
            tasks.put(saved.id(), saved);
            return saved;
        }

        @Override
        public Optional<FlowTask> findTask(String tenantId, Long taskId) {
            return Optional.ofNullable(tasks.get(taskId)).filter(value -> value.tenantId().equals(tenantId));
        }

        @Override
        public List<FlowTask> findTasksByAssignee(String tenantId, String assigneeId,
                                                  FlowEnums.TaskStatus status, int offset, int limit) {
            return tasks.values().stream().filter(value -> value.tenantId().equals(tenantId)
                    && value.assigneeId().equals(assigneeId))
                    .filter(value -> status == null || value.status() == status).skip(offset).limit(limit).toList();
        }

        @Override
        public List<FlowTask> findTasksByInstance(String tenantId, Long instanceId) {
            return tasks.values().stream().filter(value -> value.tenantId().equals(tenantId)
                    && value.instanceId().equals(instanceId)).toList();
        }

        @Override
        public boolean completeTask(FlowTask task) {
            FlowTask existing = tasks.get(task.id());
            if (existing == null || existing.status() != FlowEnums.TaskStatus.RUNNING) {
                return false;
            }
            tasks.put(task.id(), task);
            return true;
        }

        @Override
        public boolean transferTask(FlowTask task, String previousAssigneeId) {
            FlowTask existing = tasks.get(task.id());
            if (existing == null || existing.status() != FlowEnums.TaskStatus.RUNNING
                    || !existing.assigneeId().equals(previousAssigneeId)) {
                return false;
            }
            tasks.put(task.id(), task);
            return true;
        }

        @Override
        public List<FlowTask> findRunningTasks(String tenantId, Long instanceId, String nodeKey) {
            return tasks.values().stream().filter(task -> task.tenantId().equals(tenantId)
                    && task.instanceId().equals(instanceId) && task.nodeKey().equals(nodeKey)
                    && task.status() == FlowEnums.TaskStatus.RUNNING).toList();
        }

        List<FlowTask> runningTasks(Long instanceId) {
            return tasks.values().stream().filter(task -> task.instanceId().equals(instanceId)
                    && task.status() == FlowEnums.TaskStatus.RUNNING).toList();
        }

        @Override
        public void cancelRunningTasks(String tenantId, Long instanceId, String nodeKey) {
            findRunningTasks(tenantId, instanceId, nodeKey).forEach(task -> tasks.put(task.id(),
                    task.completed(FlowEnums.TaskStatus.CANCELLED, task.comment(), java.time.LocalDateTime.now(CLOCK))));
        }

        @Override
        public void cancelRunningTasks(String tenantId, Long instanceId) {
            runningTasks(instanceId).forEach(task -> tasks.put(task.id(),
                    task.completed(FlowEnums.TaskStatus.CANCELLED, task.comment(), java.time.LocalDateTime.now(CLOCK))));
        }

        @Override
        public Optional<Long> findCommandResult(String tenantId, String requestId, String commandType) {
            return Optional.ofNullable(commandResults.get(key(tenantId, requestId, commandType)));
        }

        @Override
        public boolean claimCommand(String tenantId, String requestId, String commandType) {
            return claimedCommands.add(key(tenantId, requestId, commandType));
        }

        @Override
        public void completeCommand(String tenantId, String requestId, String commandType, Long resultId) {
            commandResults.put(key(tenantId, requestId, commandType), resultId);
        }

        @Override
        public void appendEvent(FlowEvent event) {
            events.add(event);
        }

        @Override
        public List<FlowOutboxEvent> findDeliverableEvents(LocalDateTime now, int maxAttempts, int limit) {
            return List.of();
        }

        @Override
        public boolean claimEvent(Long eventId, int attempts, LocalDateTime lockUntil) {
            return false;
        }

        @Override
        public void markEventPublished(Long eventId, LocalDateTime publishedAt) {
        }

        @Override
        public void markEventFailed(Long eventId, String error, LocalDateTime nextAttemptAt) {
        }

        private String key(String tenantId, String requestId, String commandType) {
            return tenantId + ":" + requestId + ":" + commandType;
        }
    }
}
