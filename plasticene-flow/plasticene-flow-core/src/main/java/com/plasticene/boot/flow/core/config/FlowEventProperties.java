package com.plasticene.boot.flow.core.config;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 流程事件投递、重试和清理配置。
 * @author ZFJ
 * @since 2026-11-11
 */
@Data
@ConfigurationProperties("ptc.flow.event")
public class FlowEventProperties {
    private boolean enabled = true;
    private int workers = 4;
    private int maxRetries = 3;
    private Duration initialDelay = Duration.ofSeconds(30);
    private Duration maxDelay = Duration.ofMinutes(30);
    private Duration pollInterval = Duration.ofSeconds(1);
    private Duration lease = Duration.ofMinutes(2);
    private Duration callbackTimeout = Duration.ofSeconds(30);
    private Duration retention = Duration.ofDays(30);

    @PostConstruct
    public void validate() {
        if (workers < 1 || maxRetries < 0 || !positive(initialDelay) || !positive(maxDelay)
                || !positive(pollInterval) || !positive(lease) || !positive(callbackTimeout)
                || !positive(retention) || lease.compareTo(callbackTimeout) <= 0) {
            throw new IllegalArgumentException("流程事件配置须为正值且领取期限须大于回调超时");
        }
    }

    public Duration retryDelay(int retriesAlreadyUsed) {
        long cap = maxDelay.toMillis();
        long delay = Math.min(initialDelay.toMillis(), cap);
        for (int i = 0; i < retriesAlreadyUsed && delay < cap; i++) {
            delay = delay > cap / 2 ? cap : delay * 2;
        }
        return Duration.ofMillis(delay);
    }

    private boolean positive(Duration value) {
        return value != null && value.toMillis() > 0;
    }
}
