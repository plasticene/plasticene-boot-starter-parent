package com.plasticene.boot.flow.core.provider;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 抽象超时通知渠道，适配器负责地址解析和发送并按事件标识幂等。
 * @author ZFJ
 * @since 2026-10-22
 */
public interface FlowTimeoutNotification {
    /** 发送失败应抛异常；调用可能重试，eventId 是稳定的幂等键。 */
    void send(Message message);

    record Message(Long eventId, Long orgId, Long instanceId, Long taskId,
                   String nodeName, List<Long> recipients, String eventType,
                   String action, LocalDateTime deadline, LocalDateTime occurredAt) {}
}
