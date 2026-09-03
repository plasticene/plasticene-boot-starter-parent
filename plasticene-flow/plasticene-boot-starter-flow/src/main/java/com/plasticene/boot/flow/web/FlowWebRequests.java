package com.plasticene.boot.flow.web;

import com.plasticene.boot.flow.core.model.FlowNode;

import java.util.Map;

/**
 * 审批流 HTTP 接口的请求对象集合。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public final class FlowWebRequests {

    private FlowWebRequests() {
    }

    public record SaveModel(Long modelId, String code, String name, Long formDefinitionId, FlowNode rootNode) {
    }

    public record SaveFormDefinition(Long formDefinitionId, String code, String name, Map<String, Object> schema) {
    }

    public record StartFlow(String modelCode, String businessKey, Map<String, Object> variables,
                            Map<String, Object> formData) {
    }

    public record TaskComment(String comment) {
    }

    public record ReturnTask(String targetNodeKey, String comment) {
    }

    public record TransferTask(String newAssigneeId, String comment) {
    }

    public record CancelFlow(String reason) {
    }
}
