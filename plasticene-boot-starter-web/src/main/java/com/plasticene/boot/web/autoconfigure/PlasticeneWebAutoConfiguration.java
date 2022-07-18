package com.plasticene.boot.web.autoconfigure;

import com.plasticene.boot.common.executor.plasticeneThreadExecutor;
import com.plasticene.boot.web.core.filter.WebTraceFilter;
import com.plasticene.boot.web.core.prop.ThreadPoolProperties;
import com.plasticene.boot.web.core.prop.TraceProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/14 12:06
 */
@Configuration
@EnableConfigurationProperties({TraceProperties.class, ThreadPoolProperties.class})
public class PlasticeneWebAutoConfiguration {


    @Bean
    @ConditionalOnProperty(name = "ptc.trace.enable", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean buildTracerFilter() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setOrder(Integer.MIN_VALUE);
        filterRegistrationBean.setFilter(new WebTraceFilter());
        filterRegistrationBean.addUrlPatterns("/*");
        return filterRegistrationBean;
    }

    /**
     * 注入一个线程池bean，方便业务侧使用，该线程池使用transmittableThreadLocal实现父子线程之间的数据传递
     * @param properties
     * @return
     */
    @Bean
    public ExecutorService executorService(ThreadPoolProperties properties) {
        ExecutorService executor = new plasticeneThreadExecutor(
                properties.getCorePoolSize(),
                properties.getMaxPoolSize(),
                properties.getQueueCapacity(),
                properties.getThreadNamePrefix()
        );
        return executor;
    }


}