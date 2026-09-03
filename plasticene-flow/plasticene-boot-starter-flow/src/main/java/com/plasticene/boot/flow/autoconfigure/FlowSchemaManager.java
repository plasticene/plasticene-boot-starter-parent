package com.plasticene.boot.flow.autoconfigure;

import com.plasticene.boot.flow.core.FlowException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

/**
 * 根据配置管理审批流数据库表结构。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public class FlowSchemaManager implements InitializingBean, DisposableBean {

    private final DataSource dataSource;
    private final FlowProperties.SchemaAction action;
    private Flyway flyway;

    public FlowSchemaManager(DataSource dataSource, FlowProperties.SchemaAction action) {
        this.dataSource = dataSource;
        this.action = action;
    }

    @Override
    public void afterPropertiesSet() throws SQLException {
        if (action == FlowProperties.SchemaAction.NONE) {
            return;
        }
        String database = databaseName();
        flyway = Flyway.configure()
                .dataSource(dataSource)
                .table("ptc_flow_schema_history")
                .locations("classpath:db/flow/" + database)
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("0"))
                .cleanDisabled(action != FlowProperties.SchemaAction.CREATE_DROP)
                .load();
        switch (action) {
            case VALIDATE -> validate();
            case UPDATE -> flyway.migrate();
            case CREATE -> create();
            case CREATE_DROP -> {
                flyway.clean();
                flyway.migrate();
            }
            case NONE -> {
            }
        }
    }

    @Override
    public void destroy() {
        if (action == FlowProperties.SchemaAction.CREATE_DROP && flyway != null) {
            flyway.clean();
        }
    }

    private void validate() {
        flyway.validate();
        MigrationInfo[] pending = flyway.info().pending();
        if (pending.length > 0) {
            throw new FlowException("FLOW_SCHEMA_OUTDATED",
                    "Flow database has " + pending.length + " pending migration(s)");
        }
    }

    private void create() {
        if (flyway.info().applied().length > 0) {
            throw new FlowException("FLOW_SCHEMA_ALREADY_EXISTS", "Flow schema already exists");
        }
        flyway.migrate();
    }

    private String databaseName() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
            if (product.contains("mysql") || product.contains("mariadb")) {
                return "mysql";
            }
            if (product.contains("postgresql")) {
                return "postgresql";
            }
            throw new FlowException("FLOW_DATABASE_UNSUPPORTED",
                    "Only MySQL and PostgreSQL are supported, found: " + product);
        }
    }
}
