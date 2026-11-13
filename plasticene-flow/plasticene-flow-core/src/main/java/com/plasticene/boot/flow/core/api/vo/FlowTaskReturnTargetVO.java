package com.plasticene.boot.flow.core.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 展示当前审批待办可退回的前序节点。
 * @author ZFJ
 * @since 2026-11-02
 */
@Data
@AllArgsConstructor
public class FlowTaskReturnTargetVO {
    @Schema(description = "目标节点key")
    private String nodeKey;
    @Schema(description = "目标节点名称")
    private String nodeName;
}
