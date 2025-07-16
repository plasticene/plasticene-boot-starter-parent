package com.plasticene.boot.license.autoconfigure;

import com.plasticene.boot.license.core.LicenseCheckApplicationRunner;
import com.plasticene.boot.license.core.LicenseCreator;
import com.plasticene.boot.license.core.LicenseVerify;
import com.plasticene.boot.license.core.aop.LicenseAspect;
import com.plasticene.boot.license.core.prop.LicenseProperties;
import com.plasticene.boot.license.core.schedule.LicenseTask;
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
    public LicenseCreator licenseCreator() {
        return new LicenseCreator();
    }

    @Bean
    public LicenseVerify licenseVerify() {
        return new LicenseVerify();
    }

    @Bean
    @ConditionalOnProperty(name = "ptc.license.enable", havingValue = "true", matchIfMissing = true)
    public LicenseTask licenseTask() {
        return new LicenseTask();
    }

    @Bean
    @ConditionalOnProperty(name = "ptc.license.enable", havingValue = "true", matchIfMissing = true)
    public LicenseCheckApplicationRunner licenseCheckApplicationRunner() {
        return new LicenseCheckApplicationRunner();
    }

    @Bean
    @ConditionalOnProperty(name = "ptc.license.enable", havingValue = "true", matchIfMissing = true)
    public LicenseAspect licenseAspect() {
        return new LicenseAspect();
    }




}
