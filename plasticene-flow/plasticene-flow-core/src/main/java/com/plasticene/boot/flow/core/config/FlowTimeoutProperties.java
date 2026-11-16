package com.plasticene.boot.flow.core.config;

import lombok.Data;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 集中管理流程超时扫描、提醒投递和专属调度器参数。
 * @author ZFJ
 * @since 2026-11-16
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ptc.flow.timeout")
public class FlowTimeoutProperties {
    private boolean enabled = true;
    @Min(1)
    private long scanIntervalMs = 30000;
    @Min(1)
    private long noticeScanIntervalMs = 30000;
    @Min(1)
    private int poolSize = 2;
}
