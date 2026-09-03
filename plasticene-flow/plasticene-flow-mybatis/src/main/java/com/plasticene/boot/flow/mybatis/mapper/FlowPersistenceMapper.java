package com.plasticene.boot.flow.mybatis.mapper;

import com.plasticene.boot.flow.mybatis.data.FlowDataObjects;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 审批流持久化 MyBatis Mapper。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public interface FlowPersistenceMapper {

    int insertModel(FlowDataObjects.ModelDO model);

    int updateModel(FlowDataObjects.ModelDO model);

    FlowDataObjects.ModelDO selectModel(@Param("tenantId") Long tenantId, @Param("id") Long id);

    FlowDataObjects.ModelDO selectModelForUpdate(@Param("tenantId") Long tenantId, @Param("id") Long id);

    FlowDataObjects.ModelDO selectModelByCode(@Param("tenantId") Long tenantId, @Param("code") String code);

    List<FlowDataObjects.ModelDO> selectModels(@Param("tenantId") Long tenantId,
                                               @Param("status") Integer status,
                                               @Param("offset") int offset, @Param("limit") int limit);

    int selectMaxDefinitionVersion(@Param("tenantId") Long tenantId, @Param("modelId") Long modelId);

    int insertDefinition(FlowDataObjects.DefinitionDO definition);

    FlowDataObjects.DefinitionDO selectDefinition(@Param("tenantId") Long tenantId, @Param("id") Long id);

    List<FlowDataObjects.DefinitionDO> selectDefinitions(@Param("tenantId") Long tenantId,
                                                         @Param("modelId") Long modelId,
                                                         @Param("offset") int offset, @Param("limit") int limit);

    int insertFormDefinition(FlowDataObjects.FormDefinitionDO formDefinition);

    int updateFormDefinition(FlowDataObjects.FormDefinitionDO formDefinition);

    FlowDataObjects.FormDefinitionDO selectFormDefinition(@Param("tenantId") Long tenantId,
                                                          @Param("id") Long id);

    int insertFormInstance(FlowDataObjects.FormInstanceDO formInstance);

    FlowDataObjects.FormInstanceDO selectFormInstance(@Param("tenantId") Long tenantId,
                                                       @Param("id") Long id);

    int insertInstance(FlowDataObjects.InstanceDO instance);

    int updateInstance(FlowDataObjects.InstanceDO instance);

    FlowDataObjects.InstanceDO selectInstance(@Param("tenantId") Long tenantId, @Param("id") Long id);

    FlowDataObjects.InstanceDO selectInstanceForUpdate(@Param("tenantId") Long tenantId, @Param("id") Long id);

    List<FlowDataObjects.InstanceDO> selectInstancesByRequester(@Param("tenantId") Long tenantId,
                                                                @Param("requesterId") Long requesterId,
                                                                @Param("status") Integer status,
                                                                @Param("offset") int offset,
                                                                @Param("limit") int limit);

    int insertTask(FlowDataObjects.TaskDO task);

    FlowDataObjects.TaskDO selectTask(@Param("tenantId") Long tenantId, @Param("id") Long id);

    List<FlowDataObjects.TaskDO> selectTasksByAssignee(@Param("tenantId") Long tenantId,
                                                       @Param("assigneeId") Long assigneeId,
                                                       @Param("status") Integer status,
                                                       @Param("offset") int offset,
                                                       @Param("limit") int limit);

    List<FlowDataObjects.TaskDO> selectTasksByInstance(@Param("tenantId") Long tenantId,
                                                       @Param("instanceId") Long instanceId);

    int completeTask(FlowDataObjects.TaskDO task);

    int transferTask(@Param("task") FlowDataObjects.TaskDO task,
                     @Param("previousAssigneeId") Long previousAssigneeId);

    List<FlowDataObjects.TaskDO> selectRunningTasks(@Param("tenantId") Long tenantId,
                                                    @Param("instanceId") Long instanceId,
                                                    @Param("nodeKey") String nodeKey);

    int cancelRunningTasks(@Param("tenantId") Long tenantId, @Param("instanceId") Long instanceId,
                           @Param("nodeKey") String nodeKey);

    int cancelAllRunningTasks(@Param("tenantId") Long tenantId, @Param("instanceId") Long instanceId);

    FlowDataObjects.CommandDO selectCommand(@Param("tenantId") String tenantId,
                                            @Param("requestId") String requestId,
                                            @Param("commandType") String commandType);

    int insertCommand(FlowDataObjects.CommandDO command);

    int completeCommand(@Param("tenantId") String tenantId, @Param("requestId") String requestId,
                        @Param("commandType") String commandType, @Param("resultId") Long resultId,
                        @Param("completedAt") java.time.LocalDateTime completedAt);

    int insertEvent(FlowDataObjects.EventDO event);

    List<FlowDataObjects.EventDO> selectDeliverableEvents(@Param("now") java.time.LocalDateTime now,
                                                          @Param("maxAttempts") int maxAttempts,
                                                          @Param("limit") int limit);

    int claimEvent(@Param("id") Long id, @Param("attempts") int attempts,
                   @Param("lockUntil") java.time.LocalDateTime lockUntil);

    int markEventPublished(@Param("id") Long id, @Param("publishedAt") java.time.LocalDateTime publishedAt);

    int markEventFailed(@Param("id") Long id, @Param("lastError") String lastError,
                        @Param("nextAttemptAt") java.time.LocalDateTime nextAttemptAt);
}
