package com.plasticene.boot.flow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.plasticene.boot.flow.core.enums.FlowTimeoutNoticeEnums;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 持久化超时通知轮次及发送结果，支持独立于审批事务的有限重试。
 * @author ZFJ
 * @since 2026-10-22
 */
@Data
@TableName("flow_timeout_notice")
public class FlowTimeoutNotice {
    /**
     * 通知记录主键，同时作为通知渠道发送的稳定幂等标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 所属租户（公司）ID，用于通知记录和接收人的租户隔离。
     */
    private Long orgId;
    /**
     * 所属流程实例 ID。
     */
    private Long instanceId;
    /**
     * 触发本次通知的审批待办 ID。
     */
    private Long taskId;
    /**
     * 通知事件类型，取值见 {@link FlowTimeoutNoticeEnums.EventType}：REMINDER 为审批超时提醒，RESULT 为超时自动处理结果通知。
     */
    private String eventType;
    /**
     * 业务提醒轮次，从 1 开始；处理结果通知固定为 0，发送重试不增加轮次。
     */
    private Integer roundNumber;
    /**
     * 发送状态，取值见 {@link FlowTimeoutNoticeEnums.State}：PENDING 待发送，SENT 已发送，FAILED 重试耗尽，CANCELLED 提醒已失效。
     */
    private String state;
    /**
     * 本轮通知的投递尝试次数，最多 3 次；
     */
    private Integer attempts;
    /**
     * 最近一次投递失败或未发送原因摘要，不保存渠道原始异常中的敏感信息。
     */
    private String error;
    /**
     * 下次允许投递的时间；首次投递为通知创建时间，失败后延迟 5 分钟重试。
     */
    private LocalDateTime retryAt;
    /**
     * 业务通知记录创建时间。
     */
    private LocalDateTime createTime;
    /**
     * 通知渠道确认发送成功的时间；未发送成功时为空。
     */
    private LocalDateTime sentTime;
}
