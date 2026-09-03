package com.plasticene.boot.flow.mybatis.data;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批流数据库映射对象集合。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public final class FlowDataObjects {

    private FlowDataObjects() {
    }

    @Data
    public static class ModelDO {
        private Long id;
        private Long tenantId;
        private String code;
        private String name;
        private Integer status;
        private Long formDefinitionId;
        private String modelJson;
        private Long activeDefinitionId;
        private Integer activeVersion;
        private LocalDateTime publishedAt;
    }

    @Data
    public static class DefinitionDO {
        private Long id;
        private Long tenantId;
        private Long modelId;
        private String code;
        private String name;
        private Integer version;
        private Long formDefinitionId;
        private String definitionJson;
        private LocalDateTime publishedAt;
    }

    @Data
    public static class FormDefinitionDO {
        private Long id;
        private Long tenantId;
        private String code;
        private String name;
        private Integer version;
        private String schemaJson;
    }

    @Data
    public static class FormInstanceDO {
        private Long id;
        private Long tenantId;
        private Long definitionId;
        private Long formDefinitionId;
        private Long businessKey;
        private String dataJson;
        private LocalDateTime createdAt;
    }

    @Data
    public static class InstanceDO {
        private Long id;
        private Long tenantId;
        private Long definitionId;
        private Long requesterId;
        private Long businessKey;
        private Integer status;
        private String currentNodeKey;
        private String formInstanceId;
        private String variablesJson;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
    }

    @Data
    public static class TaskDO {
        private Long id;
        private Long tenantId;
        private Long instanceId;
        private String nodeKey;
        private String nodeName;
        private Long assigneeId;
        private String approvalOrderJson;
        private Integer orderIndex;
        private Integer approvalMode;
        private Integer status;
        private Integer requireComment;
        private String comment;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
    }

    @Data
    public static class CommandDO {
        private Long id;
        private String tenantId;
        private String requestId;
        private String commandType;
        private Long resultId;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
    }

    @Data
    public static class EventDO {
        private Long id;
        private String eventId;
        private String tenantId;
        private String eventType;
        private Long instanceId;
        private String payloadJson;
        private String status;
        private Integer attempts;
        private LocalDateTime occurredAt;
        private LocalDateTime nextAttemptAt;
        private LocalDateTime publishedAt;
        private String lastError;
    }
}
