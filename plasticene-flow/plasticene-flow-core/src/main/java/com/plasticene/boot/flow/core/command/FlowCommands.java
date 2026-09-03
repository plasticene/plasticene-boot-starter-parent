package com.plasticene.boot.flow.core.command;

import com.plasticene.boot.flow.core.model.FlowActor;
import com.plasticene.boot.flow.core.model.FlowNode;

import java.util.Map;
import java.util.Objects;

/**
 * 审批流写操作命令集合。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public final class FlowCommands {

    private FlowCommands() {
    }

    public record SaveModel(String requestId, FlowActor actor, Long modelId, String code, String name,
                            Long formDefinitionId, FlowNode rootNode) {
        public SaveModel {
            requireRequest(requestId, actor);
            requireText(code, "code");
            requireText(name, "name");
            Objects.requireNonNull(rootNode, "rootNode");
        }
    }

    public record SaveFormDefinition(String requestId, FlowActor actor, Long formDefinitionId,
                                     String code, String name, Map<String, Object> schema) {
        public SaveFormDefinition {
            requireRequest(requestId, actor);
            requireText(code, "code");
            requireText(name, "name");
            schema = schema == null ? Map.of() : Map.copyOf(schema);
        }
    }

    public record PublishModel(String requestId, FlowActor actor, Long modelId) {
        public PublishModel {
            requireRequest(requestId, actor);
            Objects.requireNonNull(modelId, "modelId");
        }
    }

    public record StartFlow(String requestId, FlowActor actor, String modelCode, String businessKey,
                            Map<String, Object> variables, Map<String, Object> formData) {
        public StartFlow {
            requireRequest(requestId, actor);
            requireText(modelCode, "modelCode");
            requireText(businessKey, "businessKey");
            variables = variables == null ? Map.of() : Map.copyOf(variables);
            formData = formData == null ? Map.of() : Map.copyOf(formData);
        }
    }

    public record ApproveTask(String requestId, FlowActor actor, Long taskId, String comment) {
        public ApproveTask {
            requireRequest(requestId, actor);
            Objects.requireNonNull(taskId, "taskId");
        }
    }

    public record RejectTask(String requestId, FlowActor actor, Long taskId, String comment) {
        public RejectTask {
            requireRequest(requestId, actor);
            Objects.requireNonNull(taskId, "taskId");
        }
    }

    public record ReturnTask(String requestId, FlowActor actor, Long taskId, String targetNodeKey, String comment) {
        public ReturnTask {
            requireRequest(requestId, actor);
            Objects.requireNonNull(taskId, "taskId");
            requireText(targetNodeKey, "targetNodeKey");
        }
    }

    public record TransferTask(String requestId, FlowActor actor, Long taskId, String newAssigneeId,
                               String comment) {
        public TransferTask {
            requireRequest(requestId, actor);
            Objects.requireNonNull(taskId, "taskId");
            requireText(newAssigneeId, "newAssigneeId");
        }
    }

    public record WithdrawFlow(String requestId, FlowActor actor, Long instanceId) {
        public WithdrawFlow {
            requireRequest(requestId, actor);
            Objects.requireNonNull(instanceId, "instanceId");
        }
    }

    public record CancelFlow(String requestId, FlowActor actor, Long instanceId, String reason) {
        public CancelFlow {
            requireRequest(requestId, actor);
            Objects.requireNonNull(instanceId, "instanceId");
            requireText(reason, "reason");
        }
    }

    private static void requireRequest(String requestId, FlowActor actor) {
        requireText(requestId, "requestId");
        Objects.requireNonNull(actor, "actor");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
