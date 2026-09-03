package com.plasticene.boot.flow.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 审批流 Starter 配置属性。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
@ConfigurationProperties(prefix = "ptc.flow")
public class FlowProperties {

    private boolean enabled = true;

    private final Database database = new Database();
    private final Outbox outbox = new Outbox();
    private final Web web = new Web();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Database getDatabase() {
        return database;
    }

    public Outbox getOutbox() {
        return outbox;
    }

    public Web getWeb() {
        return web;
    }

    public static class Database {

        private SchemaAction schemaAction = SchemaAction.VALIDATE;

        public SchemaAction getSchemaAction() {
            return schemaAction;
        }

        public void setSchemaAction(SchemaAction schemaAction) {
            this.schemaAction = schemaAction;
        }
    }

    public static class Outbox {

        private boolean enabled = true;
        private Duration initialDelay = Duration.ofSeconds(5);
        private Duration pollInterval = Duration.ofSeconds(2);
        private Duration retryInterval = Duration.ofSeconds(30);
        private int batchSize = 50;
        private int maxAttempts = 10;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getInitialDelay() {
            return initialDelay;
        }

        public void setInitialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
        }

        public Duration getPollInterval() {
            return pollInterval;
        }

        public void setPollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
        }

        public Duration getRetryInterval() {
            return retryInterval;
        }

        public void setRetryInterval(Duration retryInterval) {
            this.retryInterval = retryInterval;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }

    public static class Web {

        private boolean enabled;
        private String tenantHeader = "X-Flow-Tenant-Id";
        private String operatorHeader = "X-Flow-Operator-Id";
        private String rolesHeader = "X-Flow-Roles";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTenantHeader() {
            return tenantHeader;
        }

        public void setTenantHeader(String tenantHeader) {
            this.tenantHeader = tenantHeader;
        }

        public String getOperatorHeader() {
            return operatorHeader;
        }

        public void setOperatorHeader(String operatorHeader) {
            this.operatorHeader = operatorHeader;
        }

        public String getRolesHeader() {
            return rolesHeader;
        }

        public void setRolesHeader(String rolesHeader) {
            this.rolesHeader = rolesHeader;
        }
    }

    public enum SchemaAction {
        VALIDATE,
        UPDATE,
        CREATE,
        CREATE_DROP,
        NONE
    }
}
