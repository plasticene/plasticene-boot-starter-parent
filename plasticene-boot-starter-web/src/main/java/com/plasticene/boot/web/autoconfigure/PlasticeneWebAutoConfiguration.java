package com.plasticene.boot.web.autoconfigure;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.plasticene.boot.common.constant.OrderConstant;
import com.plasticene.boot.common.executor.PlasticeneThreadExecutor;
import com.plasticene.boot.web.core.advice.RequestBodyHandlerAdvice;
import com.plasticene.boot.web.core.advice.ResponseResultBodyAdvice;
import com.plasticene.boot.web.core.aop.ApiLogPrintAspect;
import com.plasticene.boot.web.core.filter.BodyTransferFilter;
import com.plasticene.boot.web.core.filter.WebTraceFilter;
import com.plasticene.boot.web.core.global.GlobalExceptionHandler;
import com.plasticene.boot.web.core.prop.ApiLogProperties;
import com.plasticene.boot.web.core.prop.ApiSecurityProperties;
import com.plasticene.boot.web.core.prop.ThreadPoolProperties;
import com.plasticene.boot.web.core.prop.TraceProperties;
import com.plasticene.boot.web.core.interceptor.FeignInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/14 12:06
 */
@Configuration
@EnableConfigurationProperties({TraceProperties.class, ThreadPoolProperties.class, ApiSecurityProperties.class, ApiLogProperties.class})
@PropertySource("classpath:/web-default.properties")
public class PlasticeneWebAutoConfiguration {


    @Bean
    @ConditionalOnProperty(name = "ptc.trace.enable", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean buildTracerFilter() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setOrder(OrderConstant.FILTER_TRACE);
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
        ExecutorService executor = new PlasticeneThreadExecutor(
                properties.getCorePoolSize(),
                properties.getMaxPoolSize(),
                properties.getQueueCapacity(),
                properties.getThreadNamePrefix()
        );
        return executor;
    }

    /**
     * 注入api 日志打印拦截器
     * @return
     */
    @Bean
//    @ConditionalOnProperty(name = "ptc.api.log.enable", havingValue = "true")
    public ApiLogPrintAspect apiLogPrintAspect() {
        return new ApiLogPrintAspect();
    }

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnProperty(name = "ptc.api.security.enable", havingValue = "true")
    public ResponseResultBodyAdvice responseResultBodyAdvice() {
        return new ResponseResultBodyAdvice();
    }

    @Bean
    public RequestBodyHandlerAdvice requestBodyHandlerAdvice() {
        return new RequestBodyHandlerAdvice();
    }

    @Bean
    public FeignInterceptor feignInterceptor() {
        return new FeignInterceptor();
    }

    @Bean
    @ConditionalOnProperty(name = "ptc.api.security.enable", havingValue = "true", matchIfMissing = false)
    public FilterRegistrationBean bodyTransferFilter() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setOrder(OrderConstant.FILTER_TRACE - 10);
        filterRegistrationBean.setFilter(new BodyTransferFilter());
        filterRegistrationBean.addUrlPatterns("/*");
        return filterRegistrationBean;
    }

//    @Bean
//    @ConditionalOnProperty(name = "ptc.api.security.enable", havingValue = "true", matchIfMissing = false)
//    public ApiSecurityAspect apiEncryptAspect() {
//        return new ApiSecurityAspect();
//    }

    @Bean
    public LocalDateTimeSerializer localDateTimeSerializer(JacksonProperties properties) {
        String dateFormat = properties.getDateFormat();
        if (StringUtils.isBlank(dateFormat)) {
            dateFormat = "yyyy-MM-dd HH:mm:ss";
        }
        return new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(dateFormat));
    }

    @Bean
    public LocalDateTimeDeserializer localDateTimeDeserializer(JacksonProperties properties) {
        String dateFormat = properties.getDateFormat();
        if (StringUtils.isBlank(dateFormat)) {
            dateFormat = "yyyy-MM-dd HH:mm:ss";
        }
        return new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(dateFormat));

    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer(
            LocalDateTimeSerializer localDateTimeSerializer, LocalDateTimeDeserializer localDateTimeDeserializer) {
        return builder -> {
            // 序列化
            builder.serializerByType(LocalDateTime.class, localDateTimeSerializer);
            // 反序列化
            builder.deserializerByType(LocalDateTime.class, localDateTimeDeserializer);
        };
    }



}
