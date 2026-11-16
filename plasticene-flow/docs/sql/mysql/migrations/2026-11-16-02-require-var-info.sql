-- 元数据转换完成并确认下方查询为0后执行，不能提前上线新应用。
SELECT COUNT(*) AS definitions_without_var_info FROM flow_definition WHERE var_info IS NULL;
ALTER TABLE flow_definition
    MODIFY COLUMN var_info JSON NOT NULL COMMENT '发布时的审批变量定义快照';
