CREATE TABLE IF NOT EXISTS category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    org_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(128) NOT NULL,
    seq INT,
    is_sys INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP(6) NULL,
    update_time TIMESTAMP(6) NULL,
    creator BIGINT,
    updater BIGINT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS form (
    id BIGINT NOT NULL AUTO_INCREMENT,
    org_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    status INT NOT NULL DEFAULT 1,
    remark VARCHAR(2000),
    conf TEXT,
    fields TEXT,
    create_time TIMESTAMP(6) NULL,
    update_time TIMESTAMP(6) NULL,
    creator BIGINT,
    updater BIGINT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS flow_model (
    id BIGINT NOT NULL AUTO_INCREMENT,
    org_id BIGINT NOT NULL,
    code VARCHAR(128) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(128),
    status INT NOT NULL,
    form_id BIGINT,
    model_node TEXT NOT NULL,
    active_model TEXT,
    active_definition_id BIGINT,
    active_version INT NOT NULL DEFAULT 0,
    publish_time TIMESTAMP(6) NULL,
    start_user_type INT NOT NULL DEFAULT 0,
    start_user_ids TEXT,
    start_dept_ids TEXT,
    start_role_ids TEXT,
    manager_user_ids TEXT,
    remark VARCHAR(2000),
    is_delete INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP(6) NULL,
    update_time TIMESTAMP(6) NULL,
    creator BIGINT,
    updater BIGINT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS flow_definition (
    id BIGINT NOT NULL AUTO_INCREMENT,
    model_id BIGINT NOT NULL,
    org_id BIGINT NOT NULL,
    code VARCHAR(128) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(128),
    form_id BIGINT,
    model_node TEXT NOT NULL,
    version INT NOT NULL,
    start_user_type INT NOT NULL DEFAULT 0,
    start_user_ids TEXT,
    start_dept_ids TEXT,
    start_role_ids TEXT,
    manager_user_ids TEXT,
    remark VARCHAR(2000),
    is_delete INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP(6) NULL,
    update_time TIMESTAMP(6) NULL,
    creator BIGINT,
    updater BIGINT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS flow_instance (
    id BIGINT NOT NULL AUTO_INCREMENT,
    org_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    process_id BIGINT NOT NULL,
    status INT NOT NULL,
    start_time TIMESTAMP(6) NOT NULL,
    end_time TIMESTAMP(6) NULL,
    current_node_key VARCHAR(128) NOT NULL,
    current_node_name VARCHAR(255),
    business_id BIGINT NOT NULL,
    form TEXT,
    var_map TEXT,
    category VARCHAR(128),
    create_time TIMESTAMP(6) NULL,
    update_time TIMESTAMP(6) NULL,
    creator BIGINT,
    updater BIGINT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS flow_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    org_id BIGINT NOT NULL,
    instance_id BIGINT NOT NULL,
    node_name VARCHAR(255) NOT NULL,
    node_key VARCHAR(128) NOT NULL,
    node_type INT NOT NULL,
    assignee BIGINT,
    approve_type INT NOT NULL DEFAULT 0,
    approve_mode INT,
    status INT NOT NULL,
    is_delete INT NOT NULL DEFAULT 0,
    start_time TIMESTAMP(6) NOT NULL,
    end_time TIMESTAMP(6) NULL,
    comment VARCHAR(2000),
    require_comment INT NOT NULL DEFAULT 0,
    completed_branch INT NOT NULL DEFAULT 0,
    assignee_list TEXT,
    create_time TIMESTAMP(6) NULL,
    update_time TIMESTAMP(6) NULL,
    creator BIGINT,
    updater BIGINT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS flow_form_instance (
    id BIGINT NOT NULL AUTO_INCREMENT,
    org_id BIGINT NOT NULL,
    definition_id BIGINT NOT NULL,
    form_id BIGINT,
    business_id BIGINT NOT NULL,
    data_json TEXT NOT NULL,
    create_time TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX idx_flow_form_instance_business ON flow_form_instance (org_id, business_id);

CREATE TABLE IF NOT EXISTS flow_command (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id VARCHAR(128) NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    command_type VARCHAR(64) NOT NULL,
    result_id BIGINT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_flow_command UNIQUE (tenant_id, request_id, command_type)
);

CREATE TABLE IF NOT EXISTS flow_outbox_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    instance_id BIGINT NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    occurred_at TIMESTAMP(6) NOT NULL,
    next_attempt_at TIMESTAMP(6) NULL,
    published_at TIMESTAMP(6) NULL,
    last_error VARCHAR(2000),
    PRIMARY KEY (id),
    CONSTRAINT uk_flow_outbox_event UNIQUE (event_id)
);
CREATE INDEX idx_flow_outbox_pending ON flow_outbox_event (status, next_attempt_at, occurred_at);
