package com.plasticene.boot.example.flow.filter;

import com.plasticene.boot.common.constant.OrderConstant;
import jakarta.annotation.Resource;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ZFJ
 * @date 2025/11/12
 */
@Configuration
public class FilterConfig {

    @Resource
    private AuthFilter authFilter;

    @Bean
    public FilterRegistrationBean<AuthFilter> buildAuthFilter() {
        FilterRegistrationBean<AuthFilter> filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setOrder(OrderConstant.FILTER_TRACE + 10);
        filterRegistrationBean.setFilter(authFilter);
        filterRegistrationBean.addUrlPatterns("/*");
        return filterRegistrationBean;
    }
}
