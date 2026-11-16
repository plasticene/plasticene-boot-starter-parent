-- 适用于此次改造前版本。由宿主执行；不要在应用启动时自动执行。
-- 当前所有存量记录均为 FLOW 自带表单。
ALTER TABLE flow_model
    ADD COLUMN form_mode TINYINT NOT NULL DEFAULT 0 COMMENT '表单类型：0为FLOW自带表单，1为宿主业务表单';
ALTER TABLE flow_definition
    ADD COLUMN form_mode TINYINT NOT NULL DEFAULT 0 COMMENT '表单类型：0为FLOW自带表单，1为宿主业务表单',
    ADD COLUMN var_info JSON NULL COMMENT '发布时的审批变量定义快照';
-- 下一步显式运行 FlowVarInfoMigration，依据各定义自己的 form_config 补齐元数据。
