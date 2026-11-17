# FLOW 模块接入

纯 Java 宿主引入 plasticene-boot-starter-flow；REST 宿主引入 plasticene-flow-rest 并显式引入 plasticene-boot-starter-web。版本由宿主平台父 POM 或 dependencyManagement 对齐。HTTP 默认关闭，通过 ptc.flow.http.enabled=true 开启；不再使用旧 Web 坐标或 FlowEngine 门面。

公开能力位于 core.api.service：FlowModelApi、FlowInstanceApi、FlowTaskApi、FlowQueryApi、FlowFormApi、FlowCategoryApi、FlowEventApi。请求和响应在 api.param/query/vo；不得依赖 impl。所有用户用例显式提供可信 LoginUser。

必须接入 FlowAuthorizationPolicy，将 FlowAction 和 FlowResource 映射到宿主权限。默认拒绝用户操作，功能授权之后仍执行模型管理范围、组织隔离及任务资格。示例用 isAdmin 演示管理授权，生产宿主应映射自身权限体系。

业务模型 formMode=1，默认允许无审批变量；需要业务字段时，宿主通过 FlowBusinessVariableProvider 提供字段契约，保存申请后在同一本地事务调用 startBusinessByCode/startBusinessByDefinition。businessId 是一次申请记录标识；重复申请报错，重新申请需新标识。独立表单模型 formMode=0，使用 startForm。

模型设计态可切换类型，发布后新申请使用新定义，历史实例继续读取原定义的 formMode、formConfig、varInfo。运行时不读取宿主最新变量契约或自动同步业务单据。

超时由 FlowTimeoutConfiguration 统一配置，参数前缀 ptc.flow.timeout：enabled 默认 true、scan-interval-ms/notice-scan-interval-ms 默认30000、pool-size 默认2。没有 FlowTimeoutNotification 时不启动提醒投递任务。可靠事件仍单独配置，遵守至少一次、有序交付及宿主幂等约定。

数据库部署见 [SQL说明](../sql/mysql/README.md)，Starter 不自动建表。Vue 宿主可引入 approve/my/detail/approval-region.vue，以 instanceId、taskId、embedded 等参数组合业务详情插槽 business-detail；业务数据的加载、授权和脱敏仍由宿主负责。

## 无变量业务模型

固定审批路线的业务模型无需逐个登记变量提供器，也无需在编辑页面先加载变量。未提供 `FlowBusinessVariableProvider` 或其 `getSchema` 返回 `null` 时，FLOW 使用空变量契约；提供器也可显式返回 `fields: []`。宿主提供器自身的执行异常仍会抛出，不会被当作无变量忽略。

发布仍遍历流程节点，任何业务变量引用都必须存在于宿主契约中；可信发起人等系统变量保持原规则。空契约发布为 `var_info` 快照，发起时可以提交 `{}`，但不能传入未声明的业务变量。已有发布版本与运行实例的快照不变。

## 业务变量只读加载

模型编辑页的“加载业务变量/刷新业务变量”仅调用 GET 查询，按照已保存模型的可信组织与流程编码读取宿主契约，不依赖数据库中的表单类型，也不保存草稿。流程编码在已创建模型中不可修改，表单类型切换后无需先保存即可查询业务契约；类型变更仅由暂存或发布统一保存。新模型仍按原有流程先创建基本信息，再按模型 ID 加载契约。
