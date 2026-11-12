package com.plasticene.boot.flow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 保存流程事件的不可变载荷及可靠交付状态。
 * @author ZFJ
 * @since 2026-11-12
 */
@Data
@TableName("flow_event_outbox")
public class FlowEventOutbox {
    /** 事件唯一标识，重投保持不变 */
    @TableId(type = IdType.INPUT)
    private String eventId;
    /** 所属组织ID */
    private Long orgId;
    /** 流程实例ID */
    private Long instanceId;
    /** 实例内的事件顺序号 */
    private Long sequenceNo;
    /** 业务分类编码快照 */
    private String category;
    /** 业务申请记录ID，区分大小写；Web独立发起时为空 */
    private String businessId;
    /** 发起时绑定的流程发布定义ID */
    private Long definitionId;
    /** 流程实例或任务事件类型 */
    private String eventType;
    /** 事件发生时间 */
    private LocalDateTime eventTime;
    /** 不可变事件载荷快照 */
    private String payload;
    /** PENDING待投递、PROCESSING处理中、DELIVERED成功、PAUSED暂停 */
    private String status;
    /** 本轮失败计数，首次失败后进入重试 */
    private Integer retryCount;
    /** 包含人工重投轮次的累计失败次数 */
    private Long totalFailures;
    /** 下一次允许投递的时间 */
    private LocalDateTime nextAttemptAt;
    /** 最近一次投递失败原因 */
    private String lastError;
    /** 当前领取令牌，防止过期工作者确认 */
    private String leaseToken;
    /** 当前领取期限，过期后可恢复投递 */
    private LocalDateTime leaseUntil;
    /** 业务确认接收的时间 */
    private LocalDateTime deliveredAt;
    /** 事件入库时间 */
    private LocalDateTime createdAt;
}
