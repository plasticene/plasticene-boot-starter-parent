CREATE TABLE flow_category_field (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    org_id BIGINT NOT NULL COMMENT '组织ID',
    category_id BIGINT NOT NULL COMMENT '流程分类ID',
    fields JSON NULL COMMENT '审批列表字段配置',
    create_time DATETIME NULL COMMENT '创建时间',
    update_time DATETIME NULL COMMENT '更新时间',
    creator BIGINT NULL COMMENT '创建人ID',
    updater BIGINT NULL COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_category (org_id, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin
    COMMENT='流程分类审批列表扩展配置';

CREATE TABLE flow_instance_field_value (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    org_id BIGINT NOT NULL COMMENT '组织ID',
    instance_id BIGINT NOT NULL COMMENT '流程实例ID',
    field_code VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '规范化列表字段标识',
    value_kind VARCHAR(16) COLLATE utf8mb4_bin NOT NULL COMMENT '列表值类型',
    text_value VARCHAR(512) COLLATE utf8mb4_bin NULL COMMENT '展示文本或列表选项编码',
    match_value VARCHAR(512) COLLATE utf8mb4_bin NULL COMMENT '归一化文本匹配值',
    number_value DECIMAL(18,4) NULL COMMENT '数字或金额检索值',
    date_value DATETIME NULL COMMENT '日期时间检索值',
    create_time DATETIME NULL COMMENT '创建时间',
    update_time DATETIME NULL COMMENT '更新时间',
    creator BIGINT NULL COMMENT '创建人ID',
    updater BIGINT NULL COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_instance_field (org_id, instance_id, field_code),
    KEY idx_list_number (org_id, field_code, number_value, instance_id),
    KEY idx_list_date (org_id, field_code, date_value, instance_id),
    KEY idx_list_text (org_id, field_code, match_value(128), instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin
    COMMENT='流程实例审批列表字段检索值';
