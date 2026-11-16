-- 仅用于全新专用示例/测试库（MySQL 8），已含业务工作流最新结构。

-- 已有 FLOW 数据库应评估并执行 business-workflow-starter.sql，而不是用本脚本迁移。

CREATE TABLE IF NOT EXISTS flow_category (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `org_id` BIGINT NOT NULL COMMENT '公司id',
    `name` VARCHAR(1000) NULL COMMENT '分类名称',
    `icon` VARCHAR(1000) NULL COMMENT '前端内置图标标识，空值使用默认图标',
    `code` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '组织内唯一的业务分类编码，不区分大小写且创建后不可修改。',
    `description` VARCHAR(1000) NULL COMMENT '分类描述',
    `seq` INT NULL COMMENT '序号',
    `is_sys` INT NULL COMMENT '是否系统内置  0：否  1：是',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '最后更新时间',
    `creator` BIGINT NULL COMMENT '创建者',
    `updater` BIGINT NULL COMMENT '更新者',
    UNIQUE KEY uk_flow_category_code (org_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程业务分类';

CREATE TABLE IF NOT EXISTS flow_definition (
    form_mode TINYINT NOT NULL DEFAULT 0 COMMENT '表单类型：0为FLOW自带表单，1为宿主业务表单',
    var_info JSON NULL COMMENT '发布时的审批变量定义快照',
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `model_id` BIGINT NULL COMMENT '模型id',
    `org_id` BIGINT NOT NULL COMMENT '公司id',
    `code` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL COMMENT '流程唯一标识(区分大小写)',
    `name` VARCHAR(1000) NULL COMMENT '流程名称',
    `icon` VARCHAR(1000) NULL COMMENT '前端内置图标标识，空值使用默认图标',
    `category` VARCHAR(64) NULL COMMENT '流程分类',
    `form_config` JSON NULL COMMENT '流程表单配置快照',
    `extra_setting` JSON NULL COMMENT '发布时的流程更多设置快照',
    `model_node` JSON NULL COMMENT '设计态-流程模型节点配置',
    `version` INT NULL COMMENT '当前发布版本',
    `start_user_type` INT NULL COMMENT '可发起人类型  0：全员   1：指定人员   2：指定部门    3：指定角色',
    `start_user_ids` TEXT NULL COMMENT '可发起用户',
    `start_dept_ids` TEXT NULL COMMENT '可发起部门',
    `start_role_ids` TEXT NULL COMMENT '可发起角色',
    `manager_user_ids` TEXT NULL COMMENT '管理员',
    `remark` VARCHAR(1000) NULL COMMENT '说明',
    `is_delete` INT NOT NULL DEFAULT 0 COMMENT '是否删除 0：否  1：是；删除后保留快照供已有实例使用',
    `publish_time` DATETIME(6) NULL COMMENT '发布时间',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '最后更新时间',
    `creator` BIGINT NULL COMMENT '创建者',
    `updater` BIGINT NULL COMMENT '更新者'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程发布快照';

CREATE TABLE IF NOT EXISTS flow_form (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `org_id` BIGINT NOT NULL COMMENT '公司id',
    `name` VARCHAR(1000) NULL COMMENT '表单名称',
    `icon` VARCHAR(1000) NULL COMMENT '前端内置图标标识，空值使用默认图标',
    `status` INT NULL COMMENT '状态 0：关闭  1：开启',
    `remark` VARCHAR(1000) NULL COMMENT '备注',
    `form_config` JSON NULL COMMENT '表单配置，包含 option 选项和 rule 规则。',
    `category` VARCHAR(64) NULL COMMENT '表单分类编码，关联流程分类。',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '最后更新时间',
    `creator` BIGINT NULL COMMENT '创建者',
    `updater` BIGINT NULL COMMENT '更新者'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程动态表单';

CREATE TABLE IF NOT EXISTS flow_form_change_record (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '表单变更记录ID。',
    `org_id` BIGINT NOT NULL COMMENT '所属组织ID。',
    `instance_id` BIGINT NULL COMMENT '所属流程实例ID。',
    `task_id` BIGINT NULL COMMENT '操作所属审批任务ID。',
    `node_key` VARCHAR(1000) NULL COMMENT '操作所属流程节点标识。',
    `node_name` VARCHAR(1000) NULL COMMENT '操作时的流程节点名称快照。',
    `operator_id` BIGINT NULL COMMENT '操作人ID。',
    `operator_name` VARCHAR(1000) NULL COMMENT '操作时的人员姓名快照。',
    `operation_time` DATETIME(6) NULL COMMENT '保存成功的业务操作时间。',
    `field_details` JSON NULL COMMENT '发生变化的字段明细快照。',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '最后更新时间',
    `creator` BIGINT NULL COMMENT '创建者',
    `updater` BIGINT NULL COMMENT '更新者'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单变更审计';

CREATE TABLE IF NOT EXISTS flow_instance (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `org_id` BIGINT NOT NULL COMMENT '公司id',
    `user_id` BIGINT NULL COMMENT '申请人',
    `definition_id` BIGINT NULL COMMENT '流程模型发布id',
    `status` INT NULL COMMENT '状态 0：审批中  1：审批通过  2：审批拒绝  3：已取消',
    `start_time` DATETIME(6) NULL COMMENT '开始时间',
    `end_time` DATETIME(6) NULL COMMENT '结束时间',
    `current_node_key` VARCHAR(1000) NULL COMMENT '当前节点key',
    `current_node_name` VARCHAR(1000) NULL COMMENT '当前节点名称',
    `business_id` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL DEFAULT NULL COMMENT '业务申请记录ID，区分大小写；Web独立发起时为空',
    `var_map` JSON NULL COMMENT '参数变量值',
    `category` VARCHAR(64) NULL COMMENT '流程分类',
    `event_sequence` BIGINT NOT NULL DEFAULT 0 COMMENT '实例内已分配的最大事件序号。',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '最后更新时间',
    `creator` BIGINT NULL COMMENT '创建者',
    `updater` BIGINT NULL COMMENT '更新者',
    UNIQUE KEY uk_flow_application (org_id, category, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例';

CREATE TABLE IF NOT EXISTS flow_model (
    form_mode TINYINT NOT NULL DEFAULT 0 COMMENT '表单类型：0为FLOW自带表单，1为宿主业务表单',
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `org_id` BIGINT NOT NULL COMMENT '公司id',
    `code` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL COMMENT '组织内唯一的流程编码，发布后不可修改',
    `name` VARCHAR(1000) NULL COMMENT '流程名称',
    `icon` VARCHAR(1000) NULL COMMENT '前端内置图标标识，空值使用默认图标',
    `category` VARCHAR(64) NULL COMMENT '流程分类',
    `status` INT NULL COMMENT '流程状态  0：草稿  1：发布  -1：停用',
    `form_config` JSON NULL COMMENT '流程表单配置快照',
    `extra_setting` JSON NULL COMMENT '流程更多设置',
    `model_node` JSON NULL COMMENT '设计态-流程模型节点配置',
    `active_model` JSON NULL COMMENT '运行态-流程模型节点配置',
    `active_definition_id` BIGINT NULL COMMENT '当前发布的模型定义id',
    `active_version` INT NULL COMMENT '当前发布版本',
    `publish_time` DATETIME(6) NULL COMMENT '发布时间',
    `start_user_type` INT NULL COMMENT '可发起人类型  0：全员   1：指定人员   2：指定部门    3：指定角色',
    `start_user_ids` TEXT NULL COMMENT '可发起用户',
    `start_dept_ids` TEXT NULL COMMENT '可发起部门',
    `start_role_ids` TEXT NULL COMMENT '可发起角色',
    `manager_user_ids` TEXT NULL COMMENT '管理员',
    `remark` VARCHAR(1000) NULL COMMENT '说明',
    `is_edited` INT NOT NULL DEFAULT 0 COMMENT '是否编辑过 0：否  1：是',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '最后更新时间',
    `creator` BIGINT NULL COMMENT '创建者',
    `updater` BIGINT NULL COMMENT '更新者',
    UNIQUE KEY uk_flow_model_code (org_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程设计模型';

CREATE TABLE IF NOT EXISTS flow_task (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '审批任务主键。',
    `org_id` BIGINT NOT NULL COMMENT '公司id',
    `instance_id` BIGINT NULL COMMENT '流程实例id',
    `node_name` VARCHAR(1000) NULL COMMENT '节点名称',
    `node_key` VARCHAR(1000) NULL COMMENT '节点key',
    `node_type` INT NULL COMMENT '节点类型',
    `assignee` BIGINT NULL COMMENT '处理人',
    `source_task_id` BIGINT NULL COMMENT '转交来源任务ID；非转交创建的任务为空。',
    `transfer_to` BIGINT NULL COMMENT '转交接收人ID；仅已转交记录有值，未转交时为空。',
    `approve_type` INT NULL COMMENT '审批类型 0：人工审批 1：自动通过 2：自动拒绝',
    `approve_mode` INT NULL COMMENT '多人审批方式 0：会签  1：或签  2：顺序审批',
    `status` INT NULL COMMENT '任务状态：0处理中，1已审批，2拒绝，3已取消，4已转交，5已退回，6等待加签',
    `is_delete` INT NOT NULL DEFAULT 0 COMMENT '是否删除  0：否  1；是',
    `start_time` DATETIME(6) NULL COMMENT '开始时间',
    `end_time` DATETIME(6) NULL COMMENT '结束时间',
    `comment` VARCHAR(1000) NULL COMMENT '审批意见',
    `require_comment` INT NULL COMMENT '是否需要填写审批意见  0：否  1：是',
    `completed_branch` INT NULL COMMENT '并行分支完成的分支数',
    `assignee_list` TEXT NULL COMMENT '多人顺序审批时存放处理人',
    `timeout_rule` JSON NULL COMMENT '待办生效的超时规则快照；由发布版本的流程默认规则或节点自定义规则解析得到。',
    `deadline` DATETIME(6) NULL COMMENT '自动处理截止时间，从当前待办创建时刻计算；未开启自动处理时为空。',
    `next_remind_time` DATETIME(6) NULL COMMENT '下次计划生成业务提醒的时间；未开启提醒、轮次用尽或超时已处理时为空。',
    `remind_count` INT NULL COMMENT '已生成的业务提醒轮次，初始为 0；与渠道是否发送成功、发送重试次数无关。',
    `timeout_handled` BOOLEAN NULL COMMENT '是否由系统超时自动处理：false 否，true 是；用于区分人工审批与系统决定。',
    `return_target_node_key` VARCHAR(1000) NULL COMMENT '退回目标节点key；仅退回操作记录有值。',
    `return_task_id` BIGINT NULL COMMENT '使该记录失效的退回任务ID；为空表示仍有效。',
    `add_sign_id` BIGINT NULL COMMENT '创建该任务的加签批次ID；非加签任务为空。',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '最后更新时间',
    `creator` BIGINT NULL COMMENT '创建者',
    `updater` BIGINT NULL COMMENT '更新者'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批执行任务';

CREATE TABLE IF NOT EXISTS flow_task_add_sign (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '加签批次ID。',
    `org_id` BIGINT NOT NULL COMMENT '所属组织ID。',
    `instance_id` BIGINT NULL COMMENT '所属流程实例ID。',
    `node_key` VARCHAR(1000) NULL COMMENT '加签所属审批节点key。',
    `source_task_id` BIGINT NULL COMMENT '发起本次加签的原审批任务ID。',
    `initiator_id` BIGINT NULL COMMENT '加签发起人ID。',
    `sign_type` VARCHAR(20) NULL COMMENT '加签类型：BEFORE前加签，AFTER后加签，PARALLEL并加签。',
    `approve_mode` INT NULL COMMENT '本批次审批方式：0会签，1或签，2顺序审批；并加签记录节点方式。',
    `assignee_ids` TEXT NULL COMMENT '本批次加签人员ID有序列表，逗号分隔，顺序审批按列表顺序执行。',
    `comment` VARCHAR(1000) NULL COMMENT '加签原因；后加签同时作为原审批人的同意意见。',
    `state` VARCHAR(20) NULL COMMENT '批次状态：RUNNING处理中，PASSED已通过，REJECTED已拒绝，CANCELLED已取消，APPLIED并加签已加入。',
    `paused_at` DATETIME(6) NULL COMMENT '原任务开始暂停的时间；仅前后加签填写，用于恢复剩余超时。',
    `end_time` DATETIME(6) NULL COMMENT '批次结束时间；并加签为人员加入完成时间。',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '最后更新时间',
    `creator` BIGINT NULL COMMENT '创建者',
    `updater` BIGINT NULL COMMENT '更新者'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批加签记录';

CREATE TABLE IF NOT EXISTS flow_timeout_notice (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '通知记录主键，同时作为通知渠道发送的稳定幂等标识。',
    `org_id` BIGINT NOT NULL COMMENT '所属租户（公司）ID，用于通知记录和接收人的租户隔离。',
    `instance_id` BIGINT NULL COMMENT '所属流程实例 ID。',
    `task_id` BIGINT NULL COMMENT '触发本次通知的审批待办 ID。',
    `event_type` VARCHAR(1000) NULL COMMENT '通知事件类型，取值见 FlowTimeoutNoticeEnums.EventType：REMINDER 为审批超时提醒，RESULT 为超时自动处理结果通知。',
    `round_number` INT NULL COMMENT '业务提醒轮次，从 1 开始；处理结果通知固定为 0，发送重试不增加轮次。',
    `state` VARCHAR(1000) NULL COMMENT '发送状态，取值见 FlowTimeoutNoticeEnums.State：PENDING 待发送，SENT 已发送，FAILED 重试耗尽，CANCELLED 提醒已失效。',
    `attempts` INT NULL COMMENT '本轮通知的投递尝试次数，最多 3 次；',
    `error` VARCHAR(1000) NULL COMMENT '最近一次投递失败或未发送原因摘要，不保存渠道原始异常中的敏感信息。',
    `retry_at` DATETIME(6) NULL COMMENT '下次允许投递的时间；首次投递为通知创建时间，失败后延迟 5 分钟重试。',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '业务通知记录创建时间。',
    `sent_time` DATETIME(6) NULL COMMENT '通知渠道确认发送成功的时间；未发送成功时为空。'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='超时业务通知';

CREATE TABLE IF NOT EXISTS flow_event_outbox (
    event_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '事件唯一标识，重投保持不变',
    org_id BIGINT NOT NULL COMMENT '所属组织ID',
    instance_id BIGINT NOT NULL COMMENT '流程实例ID',
    sequence_no BIGINT NOT NULL COMMENT '实例内的事件顺序号',
    category VARCHAR(64) NOT NULL COMMENT '业务分类编码快照',
    business_id VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NULL DEFAULT NULL COMMENT '业务申请记录ID，区分大小写；Web独立发起时为空',
    definition_id BIGINT NOT NULL COMMENT '发起时绑定的流程发布定义ID',
    event_type VARCHAR(32) NOT NULL COMMENT '流程实例或任务事件类型',
    event_time DATETIME(6) NOT NULL COMMENT '事件发生时间',
    payload JSON NOT NULL COMMENT '不可变事件载荷快照',
    status VARCHAR(16) NOT NULL COMMENT 'PENDING待投递、PROCESSING处理中、DELIVERED成功、PAUSED暂停',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '本轮失败计数，首次失败后进入重试',
    total_failures BIGINT NOT NULL DEFAULT 0 COMMENT '包含人工重投轮次的累计失败次数',
    next_attempt_at DATETIME(6) NOT NULL COMMENT '下一次允许投递的时间',
    last_error VARCHAR(1000) NULL COMMENT '最近一次投递失败原因',
    lease_token VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '当前领取令牌，防止过期工作者确认',
    lease_until DATETIME(6) NULL COMMENT '当前领取期限，过期后可恢复投递',
    delivered_at DATETIME(6) NULL COMMENT '业务确认接收的时间',
    created_at DATETIME(6) NOT NULL COMMENT '事件入库时间',
    PRIMARY KEY (event_id),
    UNIQUE KEY uk_flow_event_order (instance_id, sequence_no),
    KEY idx_flow_event_due (status, next_attempt_at),
    KEY idx_flow_event_lease (status, lease_until),
    KEY idx_flow_event_cleanup (status, delivered_at),
    KEY idx_flow_event_org (org_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务审批流程事件的可靠交付记录';


CREATE TABLE IF NOT EXISTS leave_application (
    org_id BIGINT NOT NULL COMMENT '所属组织ID',
    application_id VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL COMMENT '一次请假申请的业务记录ID',
    applicant_id BIGINT NOT NULL COMMENT '请假申请人ID',
    days INT NOT NULL COMMENT '请假天数',
    reason VARCHAR(1000) NOT NULL COMMENT '请假原因',
    instance_id BIGINT NULL COMMENT '本次申请关联的唯一流程实例ID',
    status VARCHAR(20) NOT NULL COMMENT 'SUBMITTED审批中、EFFECTIVE生效、REJECTED拒绝、CANCELLED取消',
    PRIMARY KEY (org_id, application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='示例业务自己的请假申请记录';

CREATE TABLE IF NOT EXISTS leave_event_inbox (
    event_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '用于业务接收去重的流程事件ID',
    org_id BIGINT NOT NULL COMMENT '所属组织ID',
    instance_id BIGINT NOT NULL COMMENT '流程实例ID',
    application_id VARCHAR(128) NOT NULL COMMENT '业务请假申请记录ID',
    event_type VARCHAR(32) NOT NULL COMMENT '接收的流程事件类型',
    processed BOOLEAN NOT NULL DEFAULT FALSE COMMENT '业务是否已处理成功',
    received_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '业务持久接收时间',
    PRIMARY KEY (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='示例业务自己保障的可靠事件收件箱';

CREATE TABLE IF NOT EXISTS leave_notification (
    event_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '通知来源事件ID，用于业务去重',
    org_id BIGINT NOT NULL COMMENT '所属组织ID',
    application_id VARCHAR(128) NOT NULL COMMENT '业务请假申请记录ID',
    recipient_id BIGINT NOT NULL COMMENT '接收通知的申请人ID',
    message VARCHAR(1000) NOT NULL COMMENT '请假审批结果通知内容',
    sent BOOLEAN NOT NULL DEFAULT FALSE COMMENT '示例通知渠道是否已发送成功',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '通知生成时间',
    PRIMARY KEY (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='示例业务自己的通知及重试记录';

-- 仅创建演示组织 1 的流程；已有同编码配置不自动覆盖。
INSERT INTO flow_category (id, org_id, code, name, seq, is_sys)
VALUES (90000, 1, 'leave', '请假审批', 1, 0) ON DUPLICATE KEY UPDATE id = id;
SET @leave_model = '{"key":"start","name":"员工提交","type":0,"childNode":{"key":"department-approval","name":"部门负责人审批","type":1,"approveType":0,"approveMode":1,"assigneeType":0,"assigneeList":[2],"selfApprove":0,"assigneeEmpty":1,"requireComment":0,"childNode":{"key":"end","name":"结束","type":-1}}}';
INSERT INTO flow_model (form_mode, id, org_id, code, name, category, status, model_node, active_model,
    active_definition_id, active_version, start_user_type, manager_user_ids, extra_setting, publish_time)
VALUES (1, 90001, 1, 'leave-approval', '示例请假审批', 'leave', 1, @leave_model, @leave_model,
    90002, 1, 0, '2', '{}', CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE id = id;
INSERT INTO flow_definition (form_mode, var_info, id, model_id, org_id, code, name, category, model_node,
    version, start_user_type, manager_user_ids, extra_setting, is_delete, publish_time)
VALUES (1, JSON_OBJECT('fields', JSON_ARRAY(JSON_OBJECT('field','days','title','请假天数','type',1,'required',true), JSON_OBJECT('field','reason','title','请假原因','type',0,'required',true))), 90002, 90001, 1, 'leave-approval', '示例请假审批', 'leave', @leave_model,
    1, 0, '2', '{}', 0, CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE id = id;
