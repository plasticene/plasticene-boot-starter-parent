# FLOW 模块接入

纯 Java 宿主引入 plasticene-boot-starter-flow；REST 宿主引入 plasticene-flow-rest 并显式引入 plasticene-boot-starter-web。版本由宿主平台父 POM 或 dependencyManagement 对齐。HTTP 默认关闭，通过 ptc.flow.http.enabled=true 开启；不再使用旧 Web 坐标或 FlowEngine 门面。

公开能力位于 core.api.service：FlowModelOperations、FlowInstanceOperations、FlowTaskOperations、FlowQueries、FlowFormOperations、FlowCategoryOperations、FlowEventOperations。请求和响应在 api.param/query/vo；不得依赖 impl。所有用户用例显式提供可信 LoginUser。

必须接入 FlowAuthorizationPolicy，将 FlowAction 和 FlowResource 映射到宿主权限。默认拒绝用户操作，功能授权之后仍执行模型管理范围、组织隔离及任务资格。示例用 isAdmin 演示管理授权，生产宿主应映射自身权限体系。

业务模型 formMode=1，宿主通过 FlowBusinessVariableProvider 提供字段契约，保存申请后在同一本地事务调用 startBusinessByCode/startBusinessByDefinition。businessId 是一次申请记录标识；重复申请报错，重新申请需新标识。独立表单模型 formMode=0，使用 startForm。

模型设计态可切换类型，发布后新申请使用新定义，历史实例继续读取原定义的 formMode、formConfig、varInfo。运行时不读取宿主最新变量契约或自动同步业务单据。

超时由 FlowTimeoutConfiguration 统一配置，参数前缀 ptc.flow.timeout：enabled 默认 true、scan-interval-ms/notice-scan-interval-ms 默认30000、pool-size 默认2。没有 FlowTimeoutNotification 时不启动提醒投递任务。可靠事件仍单独配置，遵守至少一次、有序交付及宿主幂等约定。

数据库部署见 [SQL说明](../sql/mysql/README.md)，Starter 不自动建表。Vue 宿主可引入 approve/my/detail/approval-region.vue，以 instanceId、taskId、embedded 等参数组合业务详情插槽 business-detail；业务数据的加载、授权和脱敏仍由宿主负责。
