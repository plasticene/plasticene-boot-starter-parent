package com.plasticene.boot.license.autoconfigure;

import com.plasticene.boot.license.core.prop.LicenseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/29 10:07
 */
@Configuration
@ComponentScan(basePackages = {"com.plasticene.boot.license"})
@EnableConfigurationProperties(LicenseProperties.class)
public class LicenseAutoConfiguration {
}
