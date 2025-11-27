package com.plasticene.boot.flow.autoconfigure;

import com.plasticene.boot.flow.core.executor.DefaultProcessExecutor;
import com.plasticene.boot.flow.core.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.provider.DefaultFlowTaskAssigneeProvider;
import com.plasticene.boot.flow.core.provider.FlowTaskAssigneeProvider;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author ZFJ
 * @date 2025/9/3
 */
@Configuration
@ComponentScan(basePackages = {"com.plasticene.boot.flow"})
@MapperScan(basePackages = {"com.plasticene.boot.flow.core.dao"})
public class FlowAutoConfiguration {

    @Bean
    public FlowTaskAssigneeProvider flowTaskAssigneeProvider () {
        return new DefaultFlowTaskAssigneeProvider();
    }

    @Bean
    public ProcessExecutor processExecutor () {
        return new DefaultProcessExecutor();
    }


}
