# 审批业务字段接入

审批列表业务字段由可选的 `plasticene-flow-business-fields` 模块提供，版本由项目统一管理。

1. 在目标 MySQL 数据库执行 [flow-approval-list-fields.sql](../sql/flow-approval-list-fields.sql)。
2. 添加 `com.plasticene.boot:plasticene-flow-business-fields` 依赖。
3. 设置 `ptc.flow.business-fields.enabled: true` 并重启。展示和搜索字段上限分别由 `max-display-fields` 与 `max-search-fields` 设置，默认均为 5。
4. 发布流程模型，再到流程分类管理中选择列表展示字段和搜索字段。业务表单由宿主声明 `allowInList`。流程表单使用发布时计算的字段资格。

四类个人审批分页统一使用 POST，扩展开关控制动态条件和列。旧模型和申请沿用基本列表。重新发布后，从新定义发起的申请才会生成检索值。示例应用可使用 `business-fields` profile 开启扩展。

启用后按部署持续使用。停用期间检索值不更新，重新启用前需要单独处理检索数据同步。
