package com.plasticene.boot.example.flow.leave;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 按需调度示例业务自己的生效和通知处理。
 * @author ZFJ
 * @since 2026-11-11
 */
@Profile("leave")
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "leave.business.scheduling-enabled", matchIfMissing = true)
public class LeaveSchedulingConfiguration {
}
