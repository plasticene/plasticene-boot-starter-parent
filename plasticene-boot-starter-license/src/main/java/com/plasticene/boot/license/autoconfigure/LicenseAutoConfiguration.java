package com.plasticene.boot.license.autoconfigure;

import com.plasticene.boot.license.core.LicenseCheckApplicationRunner;
import com.plasticene.boot.license.core.aop.LicenseAspect;
import com.plasticene.boot.license.core.prop.LicenseProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
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


    @Bean
    @ConditionalOnProperty(name = "ptc.license.start-check", havingValue = "true", matchIfMissing = true)
    public LicenseCheckApplicationRunner licenseCheckApplicationRunner() {
        return new LicenseCheckApplicationRunner();
    }

    @Bean
    public LicenseAspect licenseAspect() {
        return new LicenseAspect();
    }




}
