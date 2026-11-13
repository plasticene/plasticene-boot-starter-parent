package com.plasticene.boot.flow.core.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 对外展示超时通知快照，隔离持久化实体。
 * @author ZFJ
 * @since 2026-11-11
 */
@Data
@Schema(description = "流程超时通知记录")
public class FlowTimeoutNoticeVO {
    @Schema(description = "超时通知记录ID")
    private Long id;
    @Schema(description = "所属组织ID")
    private Long orgId;
    @Schema(description = "流程实例ID")
    private Long instanceId;
    @Schema(description = "审批任务ID")
    private Long taskId;
    @Schema(description = "超时通知类型")
    private String eventType;
    @Schema(description = "业务提醒轮次")
    private Integer roundNumber;
    @Schema(description = "通知发送状态")
    private String state;
    @Schema(description = "通知发送尝试次数")
    private Integer attempts;
    @Schema(description = "最近失败原因")
    private String error;
    @Schema(description = "下次允许投递时间")
    private LocalDateTime retryAt;
    @Schema(description = "通知创建时间")
    private LocalDateTime createTime;
    @Schema(description = "通知发送成功时间")
    private LocalDateTime sentTime;
}
