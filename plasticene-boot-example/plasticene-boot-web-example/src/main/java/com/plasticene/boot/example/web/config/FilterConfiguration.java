package com.plasticene.boot.example.web.config;

import com.plasticene.boot.common.constant.OrderConstant;
import com.plasticene.boot.web.core.filter.BodyTransferFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ZFJ
 * @since 2026/7/7
 */
@Configuration
public class FilterConfiguration {

    @Bean
    public FilterRegistrationBean<BodyTransferFilter> bodyTransferFilter() {
        FilterRegistrationBean<BodyTransferFilter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setOrder(OrderConstant.FILTER_TRACE - 10);
        filterRegistrationBean.setFilter(new BodyTransferFilter());
        filterRegistrationBean.addUrlPatterns("/*");
        return filterRegistrationBean;
    }
}
