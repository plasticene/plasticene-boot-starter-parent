package com.plasticene.boot.flow.core.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程实例当前节点开始时间
 *
 * @author ZFJ
 * @since 2026-09-17
 */
@Data
public class FlowInstanceNodeTimeDTO {

    private Long instanceId;

    private LocalDateTime startTime;
}
