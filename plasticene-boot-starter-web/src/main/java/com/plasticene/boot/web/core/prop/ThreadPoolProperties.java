package com.plasticene.boot.web.core.prop;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/14 14:55
 */
@Data
@ConfigurationProperties(prefix = "async-tsk")
public class ThreadPoolProperties {
    /**
     *  线程池维护线程的最小数量.
     */
    @Value("${async-task.corePoolSize:10}")
    private int corePoolSize;
    /**
     *  线程池维护线程的最大数量
     */
    @Value("${async-task.maxPoolSize:200}")
    private int maxPoolSize;
    /**
     *  队列最大长度
     */
    @Value("${async-task.queueCapacity:10}")
    private int queueCapacity;
    /**
     *  线程池前缀
     */
    @Value("${async-task.threadNamePrefix:ptc-pool}")
    private String threadNamePrefix;
}
