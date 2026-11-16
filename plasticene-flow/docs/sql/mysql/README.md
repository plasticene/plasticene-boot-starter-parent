# FLOW 业务库部署

新库仅执行 baseline/flow-schema.sql。已有库按顺序执行 migrations/2026-11-16-01-form-mode-and-var-info.sql，然后使用宿主依赖类路径（含 JDBC 驱动）显式运行 com.plasticene.boot.flow.core.impl.migration.FlowVarInfoMigration，最后核对缺失计数为0后执行 migrations/2026-11-16-02-require-var-info.sql。

工具从 FLOW_MIGRATION_JDBC_URL、FLOW_MIGRATION_USERNAME、FLOW_MIGRATION_PASSWORD 环境变量读取专用目标连接，不打印凭据。工具只转换 form_mode=0 且 var_info 为空的历史定义，使用各自 form_config，不修改实例数据。转换失败整次回滚；请在发布窗口先完成 SQL/转换核验再启动新应用。脚本不由 Starter 自动执行。已有表结构先核对起始版本，不重复执行 ADD COLUMN。

示例库仍使用 docs/sql/leave-example.sql，它包含独立业务示例表和数据，不用于生产业务库初始化。FLOW 与业务使用同一数据源和事务管理器。

## 迁移工具的运行类路径

`FlowVarInfoMigration` 位于 core，但 core 不绑定数据库厂商驱动。直接使用 core 模块的类路径运行，可能报 `java.sql.SQLException: No suitable driver found for jdbc:mysql:...`。这表示 JDBC 没有找到可接受该 URL 的驱动，尚未建立数据库连接。

在 IDEA 的 Application 运行配置中：

1. Main class 填 `com.plasticene.boot.flow.core.impl.migration.FlowVarInfoMigration`。
2. **Use classpath of module** 选择已依赖 FLOW 和 MySQL 驱动的宿主模块。本仓库示例可选 `plasticene-boot-flow-example`，其 POM 已声明 MySQL 驱动；不要选择 `plasticene-flow-core`。
3. 在该运行配置中设置前述三个环境变量，再显式运行迁移类；不要通过启动整个宿主应用来触发迁移。

命令行同样必须提供宿主的完整运行依赖类路径，包括 FLOW、Jackson、MySQL JDBC 驱动及其依赖：

```bash
java -cp "$FLOW_MIGRATION_CLASSPATH" com.plasticene.boot.flow.core.impl.migration.FlowVarInfoMigration
```

`FLOW_MIGRATION_CLASSPATH` 由宿主构建结果提供，不能只填 core JAR。仅添加 `Class.forName("com.mysql.cj.jdbc.Driver")` 无法替代缺失的驱动 JAR。若驱动已在类路径仍报相同错误，再检查 URL 是否以 `jdbc:mysql://` 开头，以及运行配置是否实际使用了所选宿主模块的运行依赖。

## 历史表单的迁移校验

迁移按审批变量的提取范围检查字段标识、名称和重复字段，排除不参与审批变量的子表单字段。主表单与子表单可出现同名字段，不因此改写历史编码。

历史选项按原值保存到变量快照，不追溯应用当前表单保存时的选项完整性规则。例如旧多选项包含空字符串值，迁移不会删除选项、生成替代值或修改 `form_config`。新表单保存与发布仍执行原有完整校验。转换失败时异常包含 `definitionId` 和具体原因，整次迁移仍回滚。
