package com.plasticene.boot.flow.autoconfigure;

import com.plasticene.boot.flow.core.FlowEngine;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 审批流 Starter 自动配置测试。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
class FlowAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FlowAutoConfiguration.class));

    @Test
    void createsEmbeddedEngineWhenDataSourceIsAvailable() {
        contextRunner.withUserConfiguration(DatabaseConfiguration.class)
                .withPropertyValues("ptc.flow.database.schema-action=none")
                .run(context -> {
                    assertThat(context).hasSingleBean(FlowEngine.class);
                    assertThat(context).hasSingleBean(FlowSchemaManager.class);
                });
    }

    @Test
    void backsOffWithoutDataSource() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(FlowEngine.class));
    }

    @Test
    void canBeDisabled() {
        contextRunner.withUserConfiguration(DatabaseConfiguration.class)
                .withPropertyValues("ptc.flow.enabled=false", "ptc.flow.database.schema-action=none")
                .run(context -> assertThat(context).doesNotHaveBean(FlowEngine.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class DatabaseConfiguration {

        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:unused");
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource dataSource) {
            org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration(
                    new Environment("test", new JdbcTransactionFactory(), dataSource));
            return new DefaultSqlSessionFactory(configuration);
        }
    }
}
