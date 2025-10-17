package com.plasticene.boot.delay.autoconfigure;

import com.plasticene.boot.delay.core.prop.DelayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author ZFJ
 * @date 2025/10/17
 */
@Configuration
@EnableConfigurationProperties({DelayProperties.class})
public class DelayAutoConfiguration {
}
