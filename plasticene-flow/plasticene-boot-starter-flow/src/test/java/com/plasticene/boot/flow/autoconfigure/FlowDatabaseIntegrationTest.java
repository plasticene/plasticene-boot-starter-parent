package com.plasticene.boot.flow.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasticene.boot.flow.core.model.FlowEnums;
import com.plasticene.boot.flow.core.model.FlowEvent;
import com.plasticene.boot.flow.core.model.FlowModel;
import com.plasticene.boot.flow.core.model.FlowNode;
import com.plasticene.boot.flow.mybatis.MybatisFlowRepository;
import com.plasticene.boot.flow.mybatis.mapper.FlowPersistenceMapper;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 审批流仓储的 MySQL 与 PostgreSQL 集成测试。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
@Testcontainers(disabledWithoutDocker = true)
class FlowDatabaseIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void persistsAndReadsModelsOnMySql() throws Exception {
        verifyRepository(MYSQL, "mysql");
    }

    @Test
    void persistsAndReadsModelsOnPostgreSql() throws Exception {
        verifyRepository(POSTGRESQL, "postgresql");
    }

    private void verifyRepository(JdbcDatabaseContainer<?> container, String database) throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                container.getJdbcUrl(), container.getUsername(), container.getPassword());
        Flyway.configure()
                .dataSource(dataSource)
                .table("ptc_flow_schema_history")
                .locations("classpath:db/flow/" + database)
                .load()
                .migrate();

        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setConfiguration(configuration);
        factoryBean.setMapperLocations(new ClassPathResource("mapper/FlowPersistenceMapper.xml"));
        SqlSessionFactory factory = factoryBean.getObject();
        FlowPersistenceMapper mapper = new SqlSessionTemplate(factory).getMapper(FlowPersistenceMapper.class);
        MybatisFlowRepository repository = new MybatisFlowRepository(
                mapper, new ObjectMapper().findAndRegisterModules());

        assertThat(columns(dataSource, "flow_model")).contains("org_id", "form_id", "model_node",
                "active_model", "publish_time", "start_user_ids", "is_delete");
        assertThat(columns(dataSource, "flow_instance")).contains("org_id", "user_id", "process_id",
                "business_id", "form", "var_map", "current_node_name");
        assertThat(columns(dataSource, "flow_task")).contains("org_id", "assignee", "approve_type",
                "approve_mode", "is_delete", "completed_branch", "assignee_list");
        assertThat(columns(dataSource, "ptc_flow_model")).isEmpty();

        FlowNode end = new FlowNode("end", "End", FlowEnums.NodeType.END,
                null, false, List.of(), null);
        FlowModel saved = repository.saveModel(new FlowModel(null, "1", "leave", "Leave",
                FlowEnums.ModelStatus.DRAFT, null, end, null, 0, null));

        assertThat(saved.id()).isPositive();
        assertThat(repository.findModel("1", saved.id(), false))
                .contains(saved);
        assertThat(repository.findModel("2", saved.id(), false))
                .isEmpty();
        assertThat(repository.findModels("1", FlowEnums.ModelStatus.DRAFT, 0, 20))
                .containsExactly(saved);

        LocalDateTime now = LocalDateTime.of(2026, 9, 3, 0, 0);
        repository.appendEvent(new FlowEvent("event-1", "1", "FLOW_MODEL_SAVED",
                saved.id(), Map.of("code", saved.code()), now));
        var outbox = repository.findDeliverableEvents(now, 10, 20);
        assertThat(outbox).hasSize(1);
        assertThat(repository.claimEvent(outbox.getFirst().id(), 0, now.plusSeconds(30))).isTrue();
        repository.markEventPublished(outbox.getFirst().id(), now);
        assertThat(repository.findDeliverableEvents(now, 10, 20)).isEmpty();
    }

    private Set<String> columns(DataSource dataSource, String tableName) throws Exception {
        Set<String> columns = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
             ResultSet resultSet = connection.getMetaData()
                     .getColumns(connection.getCatalog(), null, tableName, null)) {
            while (resultSet.next()) {
                columns.add(resultSet.getString("COLUMN_NAME").toLowerCase());
            }
        }
        return columns;
    }
}
