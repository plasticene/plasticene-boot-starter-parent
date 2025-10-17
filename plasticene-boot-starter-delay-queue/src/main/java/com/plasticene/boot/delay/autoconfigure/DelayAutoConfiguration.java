package com.plasticene.boot.delay.autoconfigure;

import com.plasticene.boot.delay.core.DistributedDelayQueue;
import com.plasticene.boot.delay.core.coordinator.Coordinator;
import com.plasticene.boot.delay.core.coordinator.RedisCoordinator;
import com.plasticene.boot.delay.core.prop.DelayProperties;
import com.plasticene.boot.delay.core.storage.RedisTaskStorage;
import com.plasticene.boot.delay.core.storage.TaskStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ZFJ
 * @date 2025/10/17
 */
@Configuration
@EnableConfigurationProperties({DelayProperties.class})
public class DelayAutoConfiguration {

    @ConditionalOnMissingBean(Coordinator.class)
    @Bean
    public Coordinator coordinator() {
        return new RedisCoordinator();
    }

    @ConditionalOnMissingBean(TaskStorage.class)
    @Bean
    public TaskStorage taskStorage() {
        return new RedisTaskStorage();
    }

    @Bean
    public DistributedDelayQueue distributedDelayQueue() {
        return new DistributedDelayQueue();
    }

}
