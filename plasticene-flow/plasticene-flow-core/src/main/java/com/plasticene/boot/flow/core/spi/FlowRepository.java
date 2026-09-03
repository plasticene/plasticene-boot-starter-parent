package com.plasticene.boot.flow.core.spi;

import com.plasticene.boot.flow.core.model.FlowDefinition;
import com.plasticene.boot.flow.core.model.FlowEnums;
import com.plasticene.boot.flow.core.model.FlowEvent;
import com.plasticene.boot.flow.core.model.FlowInstance;
import com.plasticene.boot.flow.core.model.FlowModel;
import com.plasticene.boot.flow.core.model.FlowOutboxEvent;
import com.plasticene.boot.flow.core.model.FlowTask;
import com.plasticene.boot.flow.core.model.FormInstance;
import com.plasticene.boot.flow.core.model.FormDefinition;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

/**
 * 审批流领域对象的持久化扩展接口。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public interface FlowRepository {

    FlowModel saveModel(FlowModel model);

    Optional<FlowModel> findModel(String tenantId, Long modelId, boolean forUpdate);

    Optional<FlowModel> findModelByCode(String tenantId, String code);

    List<FlowModel> findModels(String tenantId, FlowEnums.ModelStatus status, int offset, int limit);

    int nextDefinitionVersion(String tenantId, Long modelId);

    FlowDefinition saveDefinition(FlowDefinition definition);

    Optional<FlowDefinition> findDefinition(String tenantId, Long definitionId);

    List<FlowDefinition> findDefinitions(String tenantId, Long modelId, int offset, int limit);

    FormDefinition saveFormDefinition(FormDefinition formDefinition);

    Optional<FormDefinition> findFormDefinition(String tenantId, Long formDefinitionId);

    FormInstance saveFormInstance(FormInstance formInstance);

    Optional<FormInstance> findFormInstance(String tenantId, Long formInstanceId);

    FlowInstance saveInstance(FlowInstance instance);

    Optional<FlowInstance> findInstance(String tenantId, Long instanceId, boolean forUpdate);

    List<FlowInstance> findInstancesByRequester(String tenantId, String requesterId,
                                                FlowEnums.InstanceStatus status, int offset, int limit);

    FlowTask saveTask(FlowTask task);

    Optional<FlowTask> findTask(String tenantId, Long taskId);

    List<FlowTask> findTasksByAssignee(String tenantId, String assigneeId,
                                       FlowEnums.TaskStatus status, int offset, int limit);

    List<FlowTask> findTasksByInstance(String tenantId, Long instanceId);

    boolean completeTask(FlowTask task);

    boolean transferTask(FlowTask task, String previousAssigneeId);

    List<FlowTask> findRunningTasks(String tenantId, Long instanceId, String nodeKey);

    void cancelRunningTasks(String tenantId, Long instanceId, String nodeKey);

    void cancelRunningTasks(String tenantId, Long instanceId);

    Optional<Long> findCommandResult(String tenantId, String requestId, String commandType);

    boolean claimCommand(String tenantId, String requestId, String commandType);

    void completeCommand(String tenantId, String requestId, String commandType, Long resultId);

    void appendEvent(FlowEvent event);

    List<FlowOutboxEvent> findDeliverableEvents(LocalDateTime now, int maxAttempts, int limit);

    boolean claimEvent(Long eventId, int attempts, LocalDateTime lockUntil);

    void markEventPublished(Long eventId, LocalDateTime publishedAt);

    void markEventFailed(Long eventId, String error, LocalDateTime nextAttemptAt);
}
