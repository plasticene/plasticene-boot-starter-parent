# FLOW 用例边界改造结果

日期：2026-11-16。用户已确认完整方案并授权实施；未提交、未推送。初次交付未操作现有业务数据库，后续经用户明确授权执行的变量回填见下文记录。

## 已落地

- core 提供七组公开接口：FlowModelOperations、FlowInstanceOperations、FlowTaskOperations、FlowQueries、FlowFormOperations、FlowCategoryOperations、FlowEventOperations；删除旧 FlowEngine 门面及实现，REST 与示例使用相同用例边界；事件失败查询与重试也统一经过公开用例授权，删除旧事件管理入口。
- 新增 FlowAuthorizationPolicy / FlowBusinessVariableProvider。用户操作默认拒绝，组织及资源范围在内部核定；详情查询检查参与范围或明确的组织管理权限。按编码发起仍在公开入口显式检查解析后的定义授权，并有拒绝时不写入的回归测试。
- 查询/VO 归入公开 api，Controller 与公开 API 不再引用 impl；表单修改和批量删除补齐可信 actor、组织及授权校验，批量在同一事务执行。
- form_mode 为0自带表单/1业务表单；设计态可切换，发布生成新定义，目录读取生效定义；历史定义及实例不受草稿切换影响。var_info 保存发布变量定义，运行使用该版本快照。
- 日期、集合及数字依现有表达处理；业务变量严格类型校验，自带表单保留已配置的高精度 stringMode，不批量改写旧申请值。系统发起人变量只由可信上下文填充。
- 超时参数、专属线程池、扫描和提醒注册统一放在 FlowTimeoutConfiguration；配置属性独立承载；无通知适配不启动投递，关闭时取消自身注册任务，不关闭宿主共享资源。核心组件使用明确导入，执行内核不再条件退让。
- plasticene-flow-rest 直接替代旧 Web 坐标；生产运行不引入平台 Web Starter。用户另行确认 spring-boot-starter-web 仅作为 test 依赖，给原有 MockMvc 测试提供环境；版本沿用父 POM。
- 前端增加表单类型、业务变量读取与条件配置；类型切换重新发布才影响发起。提取 approval-region.vue，宿主可通过 instanceId/taskId/embedded 和 business-detail 插槽组合业务详情；业务模式不显示或编辑 FLOW 表单。
- 提供纯 FLOW MySQL 基线、分步增量及显式 FlowVarInfoMigration。历史记录统一补 FORM，仅从每个定义自己的 form_config 补 var_info，不修改实例数据、历史任务、事件或关联。
- 宿主示例迁至新接口、组织参数和显式授权；旧诊断 TestService 移到测试源集。重复业务申请仍按既有契约报错，不改成自动复用实例。

## 验证

- 根仓库 `mvn -o test` 通过。FLOW 各模块结果：

| 模块 | 测试 | 失败 | 错误 | 跳过 |
| --- | ---: | ---: | ---: | ---: |
| plasticene-flow-core | 400 | 0 | 0 | 0 |
| plasticene-boot-starter-flow | 14 | 0 | 0 | 0 |
| plasticene-flow-rest | 25 | 0 | 0 | 0 |

- FLOW 示例 `mvn -o -f plasticene-boot-example/pom.xml -pl plasticene-boot-flow-example -am test-compile -DskipTests` 通过。没有运行会连接数据库的示例测试。
- 前端直接相关26个测试文件、134项测试通过；包含原有详情与设计器回归、变量契约以及业务详情插槽不显示 FLOW 空表单/编辑按钮的新增验证。
- 扩展到39个 FLOW 测试文件时，212项通过、1项未改动的 model-assignee.test.ts 审批方式顺序断言失败。用 HEAD 源码和原有未修改测试在临时目录复现相同失败，不在本任务改写其断言。
- 应用类型检查仍失败，共14项：字典工具导出、上传模块缺失及类型、缺失的 BPM用户组/岗位模块、ConditionType.RULE。使用 HEAD 源码在独立临时目录复现，去掉文件行号后的诊断集合与当前完全一致，没有新增诊断。
- 修改的前端文件执行局部 ESLint 自动修复及流程控制花括号检查。相关测试通过，未运行全仓库 lint。
- Maven runtime dependency tree 确认 REST 运行图包含 Spring Web 注解API，但不包含平台 Web Starter、spring-boot-starter-web、spring-webmvc 或 OpenFeign；测试 Web 环境不传递给宿主。
- Controller / api 未发现 core.impl 引用。交付前分别检查源码差异、暂存区和空白错误。

## 未验证项

初次交付未执行真实 MySQL 基线/增量及业务事务联调，也未启动浏览器端到端场景。后续通过已有 JDBC 驱动完成变量回填，见下文；基线/增量 DDL、业务事务及浏览器端到端验证仍未在本任务中执行。不能将单元/Mapper 测试等同于真实数据库验证。

## 接入与部署入口

- [模块接入](../usage/flow-modules.md)
- [业务接入](../usage/business-workflow-starter.md)
- [数据库部署顺序](../sql/mysql/README.md)
- [完整方案](../architecture/flow-boundary-proposal.md)

系统设置仓库原有 AuthServiceImpl 变更未触碰；该仓库本次没有代码修改。没有创建分支或自动提交。

## 后续迁移修复与实库核验（2026-11-16）

用户明确授权连接其迁移配置指定的数据库并可写数据。只读排查发现待回填64个定义中，33～37的 hobby 多选字段含历史空选项值；定义37的主表单与子表单另有同名 amount 字段。

迁移改为按实际审批变量范围检查字段结构，排除子表单字段并保留全部历史选项原值；新表单保存/发布的完整校验保持不变。转换错误现在显示 definitionId 和具体原因，仍在整次事务内失败回滚。新增4项迁移回归测试，core及其依赖的 Maven 测试通过，core为404项、0失败、0错误。

实际运行修复后的 FlowVarInfoMigration，补齐64条 var_info；再次查询缺失数量为0。逐条比较快照与原 form_config 的转换结果一致，比较迁移前后全部定义，除 var_info 外其他列全部相同。没有执行基线或增量 DDL，没有修改原表单配置或实例数据。诊断脚本使用现有运行配置提供凭据，未将凭据写入文档或代码。
