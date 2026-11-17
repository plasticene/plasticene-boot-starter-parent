-- 模型草稿保存审批变量定义，用于编辑回显；发布版本仍使用宿主变量定义快照。
ALTER TABLE flow_model
    ADD COLUMN var_info JSON NULL COMMENT '草稿审批变量定义，供编辑回显使用' AFTER form_mode;
