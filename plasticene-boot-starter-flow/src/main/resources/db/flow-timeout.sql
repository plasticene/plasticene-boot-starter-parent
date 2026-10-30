-- MySQL 8：上线前手动执行一次。旧任务超时规则为空，不会补算超时。
ALTER TABLE flow_task
    ADD COLUMN timeout_rule JSON NULL COMMENT '待办生效超时规则快照，空表示无超时规则',
    ADD COLUMN deadline DATETIME NULL COMMENT '自动处理截止时间，未开启自动处理时为空',
    ADD COLUMN next_remind_time DATETIME NULL COMMENT '下次业务提醒时间，未开启或提醒结束时为空',
    ADD COLUMN remind_count INT NOT NULL DEFAULT 0 COMMENT '已生成业务提醒轮次，不含发送重试，与发送成功与否无关',
    ADD COLUMN timeout_handled TINYINT NOT NULL DEFAULT 0 COMMENT '是否由系统超时自动处理：0否，1是',
    ADD INDEX idx_flow_task_timeout_node (instance_id, node_key),
    ADD INDEX idx_flow_task_deadline (status, is_delete, deadline),
    ADD INDEX idx_flow_task_remind (status, is_delete, next_remind_time);

CREATE TABLE flow_timeout_notice (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '通知记录主键，同时作为渠道发送幂等标识',
    org_id BIGINT NOT NULL COMMENT '所属租户（公司）ID',
    instance_id BIGINT NOT NULL COMMENT '所属流程实例ID',
    task_id BIGINT NOT NULL COMMENT '触发通知的审批待办ID',
    event_type VARCHAR(16) NOT NULL COMMENT '通知类型：REMINDER超时提醒，RESULT自动处理结果',
    round_number INT NOT NULL COMMENT '业务提醒轮次从1开始，结果通知固定为0，重试不增加轮次',
    state VARCHAR(24) NOT NULL COMMENT '发送状态：PENDING待发送，SENT已发送，FAILED重试耗尽，CANCELLED提醒失效',
    attempts INT NOT NULL DEFAULT 0 COMMENT '本轮投递尝试次数，最多3次，未配置渠道不计数',
    error VARCHAR(255) NULL COMMENT '最近一次投递失败或未发送原因摘要，不含渠道敏感信息',
    retry_at DATETIME NOT NULL COMMENT '下次允许投递时间，首次为创建时间，失败后延迟5分钟',
    create_time DATETIME NOT NULL COMMENT '业务通知记录创建时间',
    sent_time DATETIME NULL COMMENT '渠道确认发送成功时间，未成功时为空',
    UNIQUE KEY uk_timeout_notice_round (org_id, task_id, event_type, round_number),
    KEY idx_timeout_notice_due (state, retry_at),
    KEY idx_timeout_notice_instance (org_id, instance_id)
) COMMENT='审批超时通知发送记录';
