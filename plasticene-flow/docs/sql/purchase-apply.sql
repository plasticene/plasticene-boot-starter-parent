-- 在 plasticene-admin 使用的 MySQL 数据库中手动执行；不修改已有 FLOW 表。
CREATE TABLE IF NOT EXISTS purchase_apply (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '采购申请ID',
    org_id BIGINT NOT NULL COMMENT '所属组织ID',
    request_no VARCHAR(64) NOT NULL COMMENT '提交请求标识，同次提交重试保持不变',
    title VARCHAR(100) NOT NULL COMMENT '采购标题',
    description VARCHAR(1000) NOT NULL COMMENT '采购说明',
    amount DECIMAL(18, 2) NOT NULL COMMENT '采购金额，单位元',
    category VARCHAR(32) NOT NULL COMMENT '采购类别编码',
    purposes JSON NOT NULL COMMENT '采购用途编码集合',
    urgent TINYINT NOT NULL DEFAULT 0 COMMENT '是否紧急：0否，1是',
    expected_date DATE NOT NULL COMMENT '期望到货日期',
    instance_id BIGINT NULL COMMENT '关联审批流程实例ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '业务审批状态：0审批中，1已通过，2已拒绝，3已取消',
    created_by BIGINT NOT NULL COMMENT '申请人ID',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_purchase_request (org_id, request_no),
    UNIQUE KEY uk_purchase_instance (instance_id),
    KEY idx_purchase_applicant (org_id, created_by, created_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购申请业务审批示例';
