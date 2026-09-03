package com.plasticene.boot.flow.mybatis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasticene.boot.flow.core.FlowException;
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
import com.plasticene.boot.flow.mybatis.data.FlowDataObjects;
import com.plasticene.boot.flow.mybatis.mapper.FlowPersistenceMapper;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于 MyBatis 的审批流仓储实现。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public class MybatisFlowRepository implements FlowRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final FlowPersistenceMapper mapper;
    private final ObjectMapper objectMapper;

    public MybatisFlowRepository(FlowPersistenceMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public FlowModel saveModel(FlowModel model) {
        FlowDataObjects.ModelDO data = toData(model);
        if (model.id() == null) {
            mapper.insertModel(data);
            return model.withId(data.getId());
        }
        mapper.updateModel(data);
        return model;
    }

    @Override
    public Optional<FlowModel> findModel(String tenantId, Long modelId, boolean forUpdate) {
        FlowDataObjects.ModelDO data = forUpdate
                ? mapper.selectModelForUpdate(numericId(tenantId, "tenantId"), modelId)
                : mapper.selectModel(numericId(tenantId, "tenantId"), modelId);
        return Optional.ofNullable(data).map(this::toModel);
    }

    @Override
    public Optional<FlowModel> findModelByCode(String tenantId, String code) {
        return Optional.ofNullable(mapper.selectModelByCode(numericId(tenantId, "tenantId"), code))
                .map(this::toModel);
    }

    @Override
    public List<FlowModel> findModels(String tenantId, FlowEnums.ModelStatus status, int offset, int limit) {
        return mapper.selectModels(numericId(tenantId, "tenantId"), modelStatus(status), offset, limit)
                .stream().map(this::toModel).toList();
    }

    @Override
    public int nextDefinitionVersion(String tenantId, Long modelId) {
        return mapper.selectMaxDefinitionVersion(numericId(tenantId, "tenantId"), modelId) + 1;
    }

    @Override
    public FlowDefinition saveDefinition(FlowDefinition definition) {
        FlowDataObjects.DefinitionDO data = toData(definition);
        mapper.insertDefinition(data);
        return definition.withId(data.getId());
    }

    @Override
    public Optional<FlowDefinition> findDefinition(String tenantId, Long definitionId) {
        return Optional.ofNullable(mapper.selectDefinition(numericId(tenantId, "tenantId"), definitionId))
                .map(this::toDefinition);
    }

    @Override
    public List<FlowDefinition> findDefinitions(String tenantId, Long modelId, int offset, int limit) {
        return mapper.selectDefinitions(numericId(tenantId, "tenantId"), modelId, offset, limit).stream()
                .map(this::toDefinition).toList();
    }

    @Override
    public FormDefinition saveFormDefinition(FormDefinition formDefinition) {
        FlowDataObjects.FormDefinitionDO data = toData(formDefinition);
        if (formDefinition.id() == null) {
            mapper.insertFormDefinition(data);
            return formDefinition.withId(data.getId());
        }
        mapper.updateFormDefinition(data);
        return formDefinition;
    }

    @Override
    public Optional<FormDefinition> findFormDefinition(String tenantId, Long formDefinitionId) {
        return Optional.ofNullable(mapper.selectFormDefinition(numericId(tenantId, "tenantId"), formDefinitionId))
                .map(this::toFormDefinition);
    }

    @Override
    public FormInstance saveFormInstance(FormInstance formInstance) {
        FlowDataObjects.FormInstanceDO data = toData(formInstance);
        mapper.insertFormInstance(data);
        return formInstance.withId(data.getId());
    }

    @Override
    public Optional<FormInstance> findFormInstance(String tenantId, Long formInstanceId) {
        return Optional.ofNullable(mapper.selectFormInstance(numericId(tenantId, "tenantId"), formInstanceId))
                .map(this::toFormInstance);
    }

    @Override
    public FlowInstance saveInstance(FlowInstance instance) {
        FlowDataObjects.InstanceDO data = toData(instance);
        if (instance.id() == null) {
            mapper.insertInstance(data);
            return instance.withId(data.getId());
        }
        mapper.updateInstance(data);
        return instance;
    }

    @Override
    public Optional<FlowInstance> findInstance(String tenantId, Long instanceId, boolean forUpdate) {
        FlowDataObjects.InstanceDO data = forUpdate
                ? mapper.selectInstanceForUpdate(numericId(tenantId, "tenantId"), instanceId)
                : mapper.selectInstance(numericId(tenantId, "tenantId"), instanceId);
        return Optional.ofNullable(data).map(this::toInstance);
    }

    @Override
    public List<FlowInstance> findInstancesByRequester(String tenantId, String requesterId,
                                                       FlowEnums.InstanceStatus status, int offset, int limit) {
        return mapper.selectInstancesByRequester(numericId(tenantId, "tenantId"),
                        numericId(requesterId, "requesterId"), instanceStatus(status), offset, limit).stream()
                .map(this::toInstance).toList();
    }

    @Override
    public FlowTask saveTask(FlowTask task) {
        FlowDataObjects.TaskDO data = toData(task);
        mapper.insertTask(data);
        return task.withId(data.getId());
    }

    @Override
    public Optional<FlowTask> findTask(String tenantId, Long taskId) {
        return Optional.ofNullable(mapper.selectTask(numericId(tenantId, "tenantId"), taskId)).map(this::toTask);
    }

    @Override
    public List<FlowTask> findTasksByAssignee(String tenantId, String assigneeId,
                                              FlowEnums.TaskStatus status, int offset, int limit) {
        return mapper.selectTasksByAssignee(numericId(tenantId, "tenantId"), numericId(assigneeId, "assigneeId"),
                        taskStatus(status), offset, limit).stream()
                .map(this::toTask).toList();
    }

    @Override
    public List<FlowTask> findTasksByInstance(String tenantId, Long instanceId) {
        return mapper.selectTasksByInstance(numericId(tenantId, "tenantId"), instanceId)
                .stream().map(this::toTask).toList();
    }

    @Override
    public boolean completeTask(FlowTask task) {
        return mapper.completeTask(toData(task)) == 1;
    }

    @Override
    public boolean transferTask(FlowTask task, String previousAssigneeId) {
        return mapper.transferTask(toData(task), numericId(previousAssigneeId, "previousAssigneeId")) == 1;
    }

    @Override
    public List<FlowTask> findRunningTasks(String tenantId, Long instanceId, String nodeKey) {
        return mapper.selectRunningTasks(numericId(tenantId, "tenantId"), instanceId, nodeKey)
                .stream().map(this::toTask).toList();
    }

    @Override
    public void cancelRunningTasks(String tenantId, Long instanceId, String nodeKey) {
        mapper.cancelRunningTasks(numericId(tenantId, "tenantId"), instanceId, nodeKey);
    }

    @Override
    public void cancelRunningTasks(String tenantId, Long instanceId) {
        mapper.cancelAllRunningTasks(numericId(tenantId, "tenantId"), instanceId);
    }

    @Override
    public Optional<Long> findCommandResult(String tenantId, String requestId, String commandType) {
        FlowDataObjects.CommandDO command = mapper.selectCommand(tenantId, requestId, commandType);
        return command == null || !"COMPLETED".equals(command.getStatus())
                ? Optional.empty() : Optional.of(command.getResultId());
    }

    @Override
    public boolean claimCommand(String tenantId, String requestId, String commandType) {
        FlowDataObjects.CommandDO command = new FlowDataObjects.CommandDO();
        command.setTenantId(tenantId);
        command.setRequestId(requestId);
        command.setCommandType(commandType);
        command.setStatus("PROCESSING");
        command.setCreatedAt(LocalDateTime.now());
        try {
            return mapper.insertCommand(command) == 1;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    @Override
    public void completeCommand(String tenantId, String requestId, String commandType, Long resultId) {
        mapper.completeCommand(tenantId, requestId, commandType, resultId, LocalDateTime.now());
    }

    @Override
    public void appendEvent(FlowEvent event) {
        FlowDataObjects.EventDO data = new FlowDataObjects.EventDO();
        data.setEventId(event.eventId());
        data.setTenantId(event.tenantId());
        data.setEventType(event.eventType());
        data.setInstanceId(event.instanceId());
        data.setPayloadJson(write(event.payload()));
        data.setStatus("PENDING");
        data.setAttempts(0);
        data.setOccurredAt(event.occurredAt());
        mapper.insertEvent(data);
    }

    @Override
    public List<FlowOutboxEvent> findDeliverableEvents(LocalDateTime now, int maxAttempts, int limit) {
        return mapper.selectDeliverableEvents(now, maxAttempts, limit).stream().map(data ->
                new FlowOutboxEvent(data.getId(), new FlowEvent(data.getEventId(), data.getTenantId(),
                        data.getEventType(), data.getInstanceId(), read(data.getPayloadJson(), MAP_TYPE),
                        data.getOccurredAt()), data.getAttempts())).toList();
    }

    @Override
    public boolean claimEvent(Long eventId, int attempts, LocalDateTime lockUntil) {
        return mapper.claimEvent(eventId, attempts, lockUntil) == 1;
    }

    @Override
    public void markEventPublished(Long eventId, LocalDateTime publishedAt) {
        mapper.markEventPublished(eventId, publishedAt);
    }

    @Override
    public void markEventFailed(Long eventId, String error, LocalDateTime nextAttemptAt) {
        String message = error == null ? "Unknown event delivery error" : error;
        mapper.markEventFailed(eventId, message.substring(0, Math.min(message.length(), 2000)), nextAttemptAt);
    }

    private FlowDataObjects.ModelDO toData(FlowModel model) {
        FlowDataObjects.ModelDO data = new FlowDataObjects.ModelDO();
        data.setId(model.id());
        data.setTenantId(numericId(model.tenantId(), "tenantId"));
        data.setCode(model.code());
        data.setName(model.name());
        data.setStatus(modelStatus(model.status()));
        data.setFormDefinitionId(model.formDefinitionId());
        data.setModelJson(write(model.modelNode()));
        data.setActiveDefinitionId(model.activeDefinitionId());
        data.setActiveVersion(model.activeVersion());
        data.setPublishedAt(model.publishedAt());
        return data;
    }

    private FlowModel toModel(FlowDataObjects.ModelDO data) {
        return new FlowModel(data.getId(), textId(data.getTenantId()), data.getCode(), data.getName(),
                modelStatus(data.getStatus()), data.getFormDefinitionId(),
                read(data.getModelJson(), FlowNode.class), data.getActiveDefinitionId(), data.getActiveVersion(),
                data.getPublishedAt());
    }

    private FlowDataObjects.DefinitionDO toData(FlowDefinition definition) {
        FlowDataObjects.DefinitionDO data = new FlowDataObjects.DefinitionDO();
        data.setId(definition.id());
        data.setTenantId(numericId(definition.tenantId(), "tenantId"));
        data.setModelId(definition.modelId());
        data.setCode(definition.code());
        data.setName(definition.name());
        data.setVersion(definition.version());
        data.setFormDefinitionId(definition.formDefinitionId());
        data.setDefinitionJson(write(definition.rootNode()));
        data.setPublishedAt(definition.publishedAt());
        return data;
    }

    private FlowDefinition toDefinition(FlowDataObjects.DefinitionDO data) {
        return new FlowDefinition(data.getId(), textId(data.getTenantId()), data.getModelId(), data.getCode(),
                data.getName(),
                data.getVersion(), data.getFormDefinitionId(), read(data.getDefinitionJson(), FlowNode.class),
                data.getPublishedAt());
    }

    private FlowDataObjects.FormDefinitionDO toData(FormDefinition form) {
        FlowDataObjects.FormDefinitionDO data = new FlowDataObjects.FormDefinitionDO();
        data.setId(form.id());
        data.setTenantId(numericId(form.tenantId(), "tenantId"));
        data.setCode(form.code());
        data.setName(form.name());
        data.setVersion(form.version());
        data.setSchemaJson(write(form.schema()));
        return data;
    }

    private FormDefinition toFormDefinition(FlowDataObjects.FormDefinitionDO data) {
        return new FormDefinition(data.getId(), textId(data.getTenantId()), data.getCode(), data.getName(),
                data.getVersion(), readMap(data.getSchemaJson()));
    }

    private FlowDataObjects.FormInstanceDO toData(FormInstance form) {
        FlowDataObjects.FormInstanceDO data = new FlowDataObjects.FormInstanceDO();
        data.setId(form.id());
        data.setTenantId(numericId(form.tenantId(), "tenantId"));
        data.setDefinitionId(form.definitionId());
        data.setFormDefinitionId(form.formDefinitionId());
        data.setBusinessKey(numericId(form.businessKey(), "businessKey"));
        data.setDataJson(write(form.data()));
        data.setCreatedAt(form.createdAt());
        return data;
    }

    private FlowDataObjects.InstanceDO toData(FlowInstance instance) {
        FlowDataObjects.InstanceDO data = new FlowDataObjects.InstanceDO();
        data.setId(instance.id());
        data.setTenantId(numericId(instance.tenantId(), "tenantId"));
        data.setDefinitionId(instance.definitionId());
        data.setRequesterId(numericId(instance.requesterId(), "requesterId"));
        data.setBusinessKey(numericId(instance.businessKey(), "businessKey"));
        data.setStatus(instanceStatus(instance.status()));
        data.setCurrentNodeKey(instance.currentNodeKey());
        data.setFormInstanceId(textId(instance.formInstanceId()));
        data.setVariablesJson(write(instance.variables()));
        data.setStartedAt(instance.startedAt());
        data.setEndedAt(instance.endedAt());
        return data;
    }

    private FormInstance toFormInstance(FlowDataObjects.FormInstanceDO data) {
        return new FormInstance(data.getId(), textId(data.getTenantId()), data.getDefinitionId(),
                data.getFormDefinitionId(), textId(data.getBusinessKey()), readMap(data.getDataJson()),
                data.getCreatedAt());
    }

    private FlowInstance toInstance(FlowDataObjects.InstanceDO data) {
        return new FlowInstance(data.getId(), textId(data.getTenantId()), data.getDefinitionId(),
                textId(data.getRequesterId()), textId(data.getBusinessKey()), instanceStatus(data.getStatus()),
                data.getCurrentNodeKey(), optionalNumericId(data.getFormInstanceId()), readMap(data.getVariablesJson()),
                data.getStartedAt(), data.getEndedAt());
    }

    private FlowDataObjects.TaskDO toData(FlowTask task) {
        FlowDataObjects.TaskDO data = new FlowDataObjects.TaskDO();
        data.setId(task.id());
        data.setTenantId(numericId(task.tenantId(), "tenantId"));
        data.setInstanceId(task.instanceId());
        data.setNodeKey(task.nodeKey());
        data.setNodeName(task.nodeName());
        data.setAssigneeId(numericId(task.assigneeId(), "assigneeId"));
        data.setApprovalOrderJson(String.join(",", task.approvalOrder()));
        data.setOrderIndex(task.orderIndex());
        data.setApprovalMode(approvalMode(task.approvalMode()));
        data.setStatus(taskStatus(task.status()));
        data.setRequireComment(task.requireComment() ? 1 : 0);
        data.setComment(task.comment());
        data.setStartedAt(task.startedAt());
        data.setEndedAt(task.endedAt());
        return data;
    }

    private FlowTask toTask(FlowDataObjects.TaskDO data) {
        List<String> approvalOrder = readStringList(data.getApprovalOrderJson());
        String assigneeId = textId(data.getAssigneeId());
        int orderIndex = Math.max(approvalOrder.indexOf(assigneeId), 0);
        return new FlowTask(data.getId(), textId(data.getTenantId()), data.getInstanceId(), data.getNodeKey(),
                data.getNodeName(), assigneeId, approvalOrder, orderIndex, approvalMode(data.getApprovalMode()),
                taskStatus(data.getStatus()), Integer.valueOf(1).equals(data.getRequireComment()), data.getComment(),
                data.getStartedAt(), data.getEndedAt());
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new FlowException("FLOW_SERIALIZATION_FAILED", exception.getMessage());
        }
    }

    private <T> T read(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new FlowException("FLOW_SERIALIZATION_FAILED", exception.getMessage());
        }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new FlowException("FLOW_SERIALIZATION_FAILED", exception.getMessage());
        }
    }

    private Map<String, Object> readMap(String value) {
        return value == null || value.isBlank() ? Map.of() : read(value, MAP_TYPE);
    }

    private List<String> readStringList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        if (value.stripLeading().startsWith("[")) {
            return read(value, STRING_LIST_TYPE);
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private Long numericId(String value, String field) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new FlowException("FLOW_IDENTIFIER_INVALID", field + " must be a numeric identifier");
        }
    }

    private String textId(Long value) {
        return value == null ? null : value.toString();
    }

    private Long optionalNumericId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer modelStatus(FlowEnums.ModelStatus value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case DRAFT -> 0;
            case PUBLISHED -> 1;
            case DISABLED -> -1;
        };
    }

    private FlowEnums.ModelStatus modelStatus(Integer value) {
        return switch (value) {
            case 0 -> FlowEnums.ModelStatus.DRAFT;
            case 1 -> FlowEnums.ModelStatus.PUBLISHED;
            case -1 -> FlowEnums.ModelStatus.DISABLED;
            default -> throw new FlowException("FLOW_STATUS_INVALID", "Unknown model status: " + value);
        };
    }

    private Integer instanceStatus(FlowEnums.InstanceStatus value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case RUNNING -> 0;
            case APPROVED -> 1;
            case REJECTED -> 2;
            case CANCELLED -> 3;
        };
    }

    private FlowEnums.InstanceStatus instanceStatus(Integer value) {
        return switch (value) {
            case 0 -> FlowEnums.InstanceStatus.RUNNING;
            case 1 -> FlowEnums.InstanceStatus.APPROVED;
            case 2 -> FlowEnums.InstanceStatus.REJECTED;
            case 3 -> FlowEnums.InstanceStatus.CANCELLED;
            default -> throw new FlowException("FLOW_STATUS_INVALID", "Unknown instance status: " + value);
        };
    }

    private Integer taskStatus(FlowEnums.TaskStatus value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case RUNNING -> 0;
            case APPROVED -> 1;
            case REJECTED -> 2;
            case RETURNED -> 3;
            case CANCELLED -> 4;
        };
    }

    private FlowEnums.TaskStatus taskStatus(Integer value) {
        return switch (value) {
            case 0 -> FlowEnums.TaskStatus.RUNNING;
            case 1 -> FlowEnums.TaskStatus.APPROVED;
            case 2 -> FlowEnums.TaskStatus.REJECTED;
            case 3 -> FlowEnums.TaskStatus.RETURNED;
            case 4 -> FlowEnums.TaskStatus.CANCELLED;
            default -> throw new FlowException("FLOW_STATUS_INVALID", "Unknown task status: " + value);
        };
    }

    private Integer approvalMode(FlowEnums.ApprovalMode value) {
        return switch (value) {
            case ALL -> 0;
            case ANY -> 1;
            case ORDER -> 2;
        };
    }

    private FlowEnums.ApprovalMode approvalMode(Integer value) {
        if (value == null) {
            return FlowEnums.ApprovalMode.ALL;
        }
        return switch (value) {
            case 0 -> FlowEnums.ApprovalMode.ALL;
            case 1 -> FlowEnums.ApprovalMode.ANY;
            case 2 -> FlowEnums.ApprovalMode.ORDER;
            default -> throw new FlowException("FLOW_APPROVAL_MODE_INVALID",
                    "Unknown approval mode: " + value);
        };
    }
}
