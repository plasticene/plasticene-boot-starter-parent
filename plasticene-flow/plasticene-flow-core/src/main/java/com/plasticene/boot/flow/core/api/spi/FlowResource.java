package com.plasticene.boot.flow.core.api.spi;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 描述宿主进行功能授权所需的流程资源。
 * @author ZFJ
 * @since 2026-11-16
 */
@Schema(description = "流程授权资源，只包含可信范围及标识")
public record FlowResource(
        @Schema(description = "组织标识") Long orgId,
        @Schema(description = "资源类型") String resourceType,
        @Schema(description = "资源标识") String resourceId,
        @Schema(description = "模型标识") Long modelId,
        @Schema(description = "发布定义标识") Long definitionId,
        @Schema(description = "实例标识") Long instanceId,
        @Schema(description = "任务标识") Long taskId,
        @Schema(description = "流程编码") String processCode,
        @Schema(description = "流程分类") String category) {
}
