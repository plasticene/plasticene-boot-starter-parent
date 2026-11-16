# FLOW 完整改造方案

日期：2026-11-16。状态：已确认并实施。已合并表单类型可切换、存量全为自带表单和字段命名决定；实际改造及验证结果见[实施结果](../refactoring/flow-boundary-implementation-result.md)。

决定依据：[ADR 0004](../adr/0004-default-http-and-dual-approval.md)。术语依据：[术语表](../domain/GLOSSARY.md)。本稿合并此前分散的方案和补充，不再保留互相矛盾的待确认描述。

## 1. 已确认的改造边界

- 保留 core / starter / rest 三模块，REST 是正式默认接口，Java 与 HTTP 共用公开用例和授权检查。
- 业务单据审批与独立表单申请在模型设计态明确区分，允许切换表单类型；重新发布后仅对新申请生效，历史定义和运行实例保持原类型及配置，两种方式共用执行内核。
- 业务单据、修改入口和业务详情归宿主；FLOW 管审批数据、执行规则和组织隔离，并提供可复用审批区域。
- 宿主提供管理授权；模型管理员继续约束具体模型的管理范围。功能授权不能替代审批资格和组织校验。
- 业务变量契约归宿主，独立表单从表单字段获得变量定义；发布保存定义，运行按对应版本校验。
- 运行实例使用已保存审批数据，不自动同步宿主单据变化。关键字段变化由宿主限制修改或取消后重新申请。
- 只开放明确的 SPI，不支持替换执行内核。沿用本地事务发起、数据库可靠事件异步交付，不重写执行算法。
- 超时参数、线程池、周期任务装配及生命周期集中到一个配置类。
- FLOW 提供纯建表基线和版本增量 SQL，宿主负责执行；Starter 默认不建库、不自动改表。
- plasticene-flow-rest 更名为 plasticene-flow-rest；平台 Web Starter 由宿主显式引入并统一版本。
- 不拆更多实现 Jar，不改生产数据库，不自动提交。生产运行不增加第三方库；用户已单独确认 REST 模块使用 spring-boot-starter-web 的 test 作用域提供现有 MockMvc 测试环境，版本沿用父 POM。

## 2. 模块及包结构

```text
plasticene-flow/
  plasticene-flow-core/
    com.plasticene.boot.flow.core/
      api/
        service/             # 完整用例接口
        param/ query/ vo/    # 公开参数、查询、结果
        spi/ event/         # 宿主接入与事件契约
      model/                 # 节点、变量定义、申请方式、枚举
      config/                # 类型化配置参数
      impl/
        application/         # 用例编排、授权、事务边界
        service/             # 保留有必要的内部持久化服务
        task/ executor/ tree/
        validation/ form/
        persistence/ event/ timeout/
  plasticene-boot-starter-flow/
    com.plasticene.boot.flow.autoconfigure/
  plasticene-flow-rest/
    com.plasticene.boot.flow.rest/
      controller/ autoconfigure/
```

依赖方向：rest → starter → core。rest 可直接声明其源码使用的 core/common/Spring API；core 不反向依赖 rest/starter。外部代码只使用公开用例、模型、DTO 和 SPI；不使用 impl。公开接口不继承 IService，不接受持久化实体、Wrapper 或执行器。

内部持久化服务可继续使用 IService；不为分层机械增加全量 Repository 和重复转发类。现有业务逻辑优先复用，迁移的是调用边界和职责。若内部 Service 本身已承担完整用例，就提取其编排职责，而不是额外复制一份逻辑。

## 3. 公开接口清单

下列方法是建议签名，待本稿确认后冻结。为简洁，表中每个方法均省略最后一个必填参数 LoginUser actor；Java 调用显式提供 actor，REST 从可信 LoginUserHolder 获取，不从请求体接受 actor。返回沿用 Long、void、PageResult 和公开 VO，不返回 HTTP ResponseVO。

### 3.1 FlowModelOperations

| 方法 | 返回 | 行为 |
| --- | --- | --- |
| create(FlowModelParam param) | Long | 创建草稿，记录申请方式与模型管理员 |
| update(FlowModelParam param) | void | 修改设计态，允许切换 formMode；不修改已发布定义或实例 |
| delete(List<Long> modelIds) | void | 保留批量原子性、版本保留和引用规则 |
| get(Long modelId) | FlowModelVO | 设计态详情 |
| page(FlowModelQuery query) | PageResult<FlowModelVO> | 按授权与组织范围查询 |
| statistics(FlowModelQuery query) | FlowModelStatisticsVO | 与列表相同范围 |
| setEnabled(FlowModelEnableParam param) | void | 启停已有发布模型 |
| publish(Long modelId) | void | 按设计态类型获取变量契约、校验并保存新定义；切换当前生效版本，不改历史定义 |
| listVersions(Long modelId) | List<FlowDefinitionVO> | 发布历史 |
| getVersion(Long modelId, Long definitionId) | FlowDefinitionVO | 校验版本属于该模型 |
| getVariableSchema(Long modelId) | FlowVariableSchema | 设计态可用字段；业务模式调用宿主，表单模式执行转换 |

保持现有 HTTP publish 的空响应，不为方便增加新的返回协议。getVariableSchema 是设计器所需新能力，仅在模型管理范围内开放。

### 3.2 FlowInstanceOperations

| 方法 | 返回 | 行为 |
| --- | --- | --- |
| startBusinessByCode(FlowBusinessStartByCodeParam param) | Long | 按组织内稳定编码解析当前可用定义，再按该定义的快照校验 |
| startBusinessByDefinition(FlowBusinessStartByDefinitionParam param) | Long | 按指定可发起定义校验及发起，不把已停用或删除版本视为天然可发起 |
| startForm(FlowFormStartParam param) | Long | 仅独立表单模式，无宿主 businessId |
| previewRoute(FlowRoutePreviewParam param) | List<FlowRouteNodeVO> | 校验权限、模式和变量定义后预估路径，不生成任务 |
| cancel(Long instanceId) | void | 申请人资格、取消策略和状态校验后取消 |

参数具体边界：

- FlowBusinessStartByCodeParam：processCode、businessId、varMap。
- FlowBusinessStartByDefinitionParam：definitionId、businessId、varMap。
- FlowFormStartParam：definitionId、varMap；可保留既有 JSON 字段名和 /flow/instance/start 路径。
- FlowRoutePreviewParam：definitionId、varMap；预估不读取宿主最新业务值。

businessId 表示一次业务申请记录的标识，不是可反复复用的业务主单标识；拒绝后重新申请仍使用新的申请记录。保留既有组织+分类下重复申请的报错行为，不在本次改造中改成自动返回旧实例。

业务入口由宿主完成业务校验和保存，在同一事务调用业务发起用例；REST 不新增允许浏览器任意提交 businessId 和审批变量的通用业务发起接口。统一用例仍负责 FLOW 自身授权、模式、组织及变量校验。

### 3.3 FlowTaskOperations

| 方法 | 返回 | 参数复用 |
| --- | --- | --- |
| approve(FlowTaskParam param) | void | 现有审批参数 |
| reject(FlowTaskParam param) | void | 现有拒绝参数 |
| saveForm(FlowTaskFormParam param) | void | 仅独立表单模式，保存允许编辑字段并记录变更 |
| addSign(FlowTaskAddSignParam param) | void | 现有加签参数 |
| transfer(FlowTaskTransferParam param) | void | 现有转交参数 |
| returnTask(FlowTaskReturnParam param) | void | 现有退回参数 |
| withdraw(Long taskId) | void | 按既有撤回规则 |
| listReturnTargets(Long taskId) | List<FlowTaskReturnTargetVO> | 校验当前任务办理资格 |

审批参数内若含表单修改，仅独立表单方式允许相应变更；业务单据方式拒绝通过审批参数间接修改宿主数据。现有前加签/后加签/并加签、有效执行路径和实例锁逻辑继续复用。

### 3.4 FlowQueries

| 方法 | 返回 | 查询范围 |
| --- | --- | --- |
| pageMyInstances(FlowInstanceQuery query) | PageResult<FlowInstanceVO> | actor 本人申请 |
| pageMyTodo/pageMyDone/pageMyCopy(FlowTaskQuery query) | PageResult<FlowTaskPageVO> | actor 本人待办/已办/抄送 |
| getInstance(Long instanceId, Long taskId) | FlowInstanceDetailVO | taskId 可空；实例参与资格或明确管理授权，叠加组织和字段规则 |
| pageInstances(FlowInstanceQuery query) | PageResult<FlowInstanceVO> | 明确授权的组织实例管理范围 |
| instanceStatistics(FlowInstanceQuery query) | FlowInstanceStatisticsVO | 与组织实例列表相同范围 |
| dashboard(FlowDashboardQuery query) | FlowDashboardVO | 明确授权的看板范围 |
| getStartableCatalog() | FlowStartCatalogVO | 符合发起资格的流程，结果包含申请方式 |
| getStartableDefinition(Long definitionId) | FlowDefinitionVO | 校验发起资格后提供发起所需定义及快照 |

查询不是无权限捷径。orgId、本人 userId 由用例从 actor 确定，不信任 Controller 注入或请求传入。模型编辑和版本历史查询留模型用例，避免通用查询接口复制所有领域查询。

### 3.5 表单、分类和事件

| 接口 | 方法与返回 |
| --- | --- |
| FlowFormOperations | Long create(FlowFormParam)；void update(FlowFormParam)；void delete(List<Long>)；FlowFormVO get(Long)；List<FlowFormVO> list()；PageResult<FlowFormVO> page(FlowFormQuery) |
| FlowCategoryOperations | Long create(FlowCategoryParam)；void update(FlowCategoryParam)；void sort(List<Long>)；void delete(List<Long>)；List<FlowCategoryVO> list()；PageResult<FlowCategoryVO> page(FlowCategoryQuery) |
| FlowEventOperations | List<FlowEventFailureVO> failures(int limit)；void retry(String eventId) |

全部同样接收 actor。批量删除先完整校验再在一个事务执行，不在 Controller 循环单条用例。分类继续保留系统分类、在用关系和编码规则。事件列表有界；重投只接受当前组织且处于允许重投状态的事件，不开放跳过事件能力。

### 3.6 数据类型整理

- impl.model.query 的 Model/Form/Category/Dashboard 查询类型移至 api.query；客户端提供的查询条件与内部授权条件分开处理。
- Definition、StartCatalog、StartableModel、ModelStatistics、InstanceStatistics、Dashboard VO 公开迁移；内部聚合查询行仍留 impl.persistence.dto。
- impl.model.vo 的 Category/Form/Model VO 当前是 api VO 的无字段子类，统一使用 api 类型；同步修改 PageResult 泛型和映射，不能依赖泛型协变。
- 事件运维不再返回 impl.event.FlowEventDelivery。FlowEventFailureVO 保留既有 eventId、orgId、instanceId、category、eventType、status、retryCount、lastError、nextAttemptAt，不新增事件载荷暴露。
- 原 FlowInstanceParam、FlowRouteParam 可整合进上述公开发起/预估参数；迁移时保留 definitionId、varMap 的 HTTP 字段名。
- 模型、定义、目录和实例详情输出申请方式；定义输出本版本变量定义。业务详情不塞入 FlowInstanceDetailVO，由宿主独立授权加载。
- 请求和响应继续使用现有 Schema 注解，后端 VO 是前端契约来源，不另加同义注释体系。

## 4. SPI 具体契约建议

### 4.1 统一授权

建议签名：boolean FlowAuthorizationPolicy.isAllowed(LoginUser actor, FlowAction action, FlowResource resource)。返回 false 或适配异常都不视为允许；用例将拒绝转换为项目既有异常。SPI 只决定宿主功能授权，不实现 FLOW 的组织隔离或任务资格规则。

FlowResource 是公开、只读的资源描述：组织、资源类型、资源标识，以及适用时的 modelId、definitionId、instanceId、taskId、processCode、category。仅提供当前操作所需信息，不暴露数据库实体、变量值或 SQL 条件。创建操作资源 ID 可空，组织来自 actor；已有资源先按 actor 组织加载，不能由请求伪造描述。

FlowAction 按实际操作枚举：MODEL_CREATE/UPDATE/DELETE/READ/PUBLISH/ENABLE、FORM_CREATE/UPDATE/DELETE/READ、CATEGORY_CREATE/UPDATE/DELETE/SORT/READ、INSTANCE_START/CANCEL/READ/READ_ORG、TASK_APPROVE/REJECT/SAVE_FORM/ADD_SIGN/TRANSFER/RETURN/WITHDRAW、DASHBOARD_READ、EVENT_READ/RETRY。版本查询和设计变量查询归模型读取，预估和可发起定义归发起能力，退回目标查询归退回能力。细粒度动作由宿主映射为现有权限标识，不固定宿主角色名称。

校验顺序：参数与可信 actor → 组织内定位资源 → 宿主授权 → 适用的模型管理员/发起资格/任务资格/状态/字段规则。写操作在锁内重新校验影响执行的状态和范围。批量操作不得只授权第一项，列表必须执行组织范围与角色对应范围约束。

无授权适配时，用户发起的受保护用例缺省拒绝；内部超时推进、事件交付不伪造用户，也不借公开用户接口绕过规则。模型管理员不能自动获得跨组织权限，宿主授予功能权限也不自动成为某个模型管理员。

已确认没有外部真实 API 调用或服务引用。FlowEventManagementPolicy 的既有调用整体迁入统一授权策略后移除旧策略，不设计过渡适配或两个策略 OR 合并。

### 4.2 业务变量定义

建议签名：FlowVariableSchema FlowBusinessVariableProvider.getSchema(Long orgId, String processCode)。以组织和稳定流程编码定位，不新增同义 businessType；分类仍负责既有业务归类和事件路由。一个宿主组合多种业务字段来源时自行在该 SPI 中路由，不引入新的插件注册框架。

FlowVariableSchema 建议为字段定义列表；字段包含 field、title、type、required，以及对应类型必须的格式/集合元素约束。它不包含布局、控件事件、业务变量取值回调。先沿用现有 FieldTypeEnum 的 STRING/NUMBER/DATE/COLLECTION；新增布尔、对象或新运算类型需补方案，不顺手扩展。

此 SPI 只在设计字段查询和业务模型发布时调用。null、重复字段、非法类型、必要格式缺失及 SPI 异常使相关查询/发布失败；空字段列表仅在流程确实不依赖业务字段时合法。独立表单模式不要求这个 SPI，由内部表单适配转换为同样的字段定义。

发布定义存储变量结构快照；业务发起与审批保存按对应快照校验，不在运行中再调用 getSchema。宿主删除最新契约字段不改变历史定义；通过编码发起解析当前有效定义，通过 ID 发起仍遵守既有版本可发起规则。宿主负责为目标定义提供符合快照的数据。

输入规则建议：缺少 required 或其值为 null 拒绝；可选字段缺失允许；出现契约之外的业务字段拒绝；NUMBER 不静默接受任意数字字符串；DATE 使用与已支持表单和运算器一致的明确格式，COLLECTION 校验元素类型。系统变量由可信上下文填充，不允许业务请求覆盖。系统变量集合、日期格式和集合值编码在实施接口清单中逐一核对既有值，不猜测改写。

上述严格校验是新增行为，必须先盘点既有 JSON 数据和前端编码；无法满足时补充明确的转换或迁移方案，不直接对历史实例追溯报错。运行中的旧实例按回填并审定的历史定义执行。

### 4.3 复用现有 SPI

| SPI | 处理方式与调用语义 |
| --- | --- |
| FlowOrganizationProvider | 新用例与内部调用统一使用带 orgId 方法；人员显示和有效人员校验区分，人员不存在不自动视为可转交。旧无组织方法及内部调用一次迁移移除，不以隐式线程上下文代替组织参数 |
| FlowTaskAssigneeProvider | 保留带 orgId、applicantId 的解析方法；沿用当前节点策略和既有空审批人处理规则，本轮不增加自定义节点执行协议 |
| FlowTimeoutNotification | 保留 send(Message)；失败抛出，由既有重试逻辑处理；eventId 用于幂等，没有适配则不启动提醒投递任务 |
| FlowEventConsumer | 保留 businessType()/receive(event)；businessType 沿用分类编码语义；至少一次、有序交付和宿主幂等责任不变 |

ProcessExecutor、DAO、内部节点校验器不转为公共 SPI。现有 ProcessExecutor 条件退让和覆盖测试证明它曾可被替换；本方案依据已确认的 Q10 明确废止该扩展约定，删除覆盖式装配分支并更新相应测试和文档，属于有意的兼容变化。

## 5. 用例实现与事务

业务发起调用链：宿主事务保存申请 → FlowInstanceOperations 授权/模式/变量校验 → 内部运行协作创建实例和任务 → 同事务写事件 → 返回实例 ID，宿主回写申请。所有步骤使用同一 DataSource 和匹配的事务管理器，异常继续传播。

任务办理调用链：Java/REST → FlowTaskOperations → 组织内定位 → 宿主授权 → 既有实例锁 → 重新校验任务/状态 → 可选表单修改 → 内部办理和推进 → 同事务记录事件。查询和预估不推进流程。

impl.application 按 model/instance/task/query/form/category/event 分包实现公开接口。现有 task 协作对象继续复用，内部 Service 留下持久化与必要内部能力；分阶段搬移编排，避免同一校验长期维护两份。新增公开用例不暴露选锁、加分支计数、更新当前节点等内部操作。

查询实现负责确定可信组织/本人范围，statistics 等方法必须接收 actor，不能继续依赖 Controller 先写 orgId。表单 Web update/delete 当前走不带 actor 的内部重载；切换后统一补齐授权与组织校验，是本方案明确的行为修正。

## 6. 自动配置与 REST

| 配置类 | 负责装配 |
| --- | --- |
| FlowAutoConfiguration | 完整用例、授权/变量适配接入、内部执行协作、Mapper、基础校验；明确 Bean/Import 清单，收窄宽泛组件扫描 |
| FlowEventConfiguration | 事件存储、消费者、交付与运维用例的必要设施 |
| FlowTimeoutConfiguration | 超时参数、专属线程池、两个周期任务和生命周期；是唯一超时自动配置类 |
| FlowRestAutoConfiguration | 已有 Servlet MVC 环境且 ptc.flow.http.enabled=true 时注册 Controller；不创建服务器或宿主身份体系 |

FlowTimeoutProperties 仅作参数载体，保留 enabled 缺省 true、两个间隔 30000ms、线程池大小 2 的现有行为。扫描类移除 @Scheduled，只保留 handleDueTasks/sendNotification 一次处理方法；配置类内声明生命周期 Bean，启动时在具名专属调度器注册任务，停止时取消自身任务并按资源归属释放线程池。不设置宿主全局调度器；没有通知 SPI 不注册投递任务，关闭超时不创建两个周期任务与专属线程池。自定义线程池复用时不关闭宿主拥有的其他资源。

将模块、目录与包更名为 plasticene-flow-rest / flow.rest；更新聚合 modules、根 dependencyManagement、示例、自动配置 imports、构建命令和文档。保留已有 /flow 路径、JSON 字段和 ptc.flow.http.enabled，不把改名变成路由迁移。

REST 移除 platform Web Starter 直接依赖，保留直接使用的 core/common/Spring Web/Context/Boot 自动配置/文档注解依赖。标准宿主显式引入 plasticene-flow-rest 和 plasticene-boot-starter-web，由平台父 POM 或宿主 dependencyManagement 统一版本；不在本次新建 BOM。使用其他 Web 集成的宿主须提供 MVC、可信身份及兼容的异常响应处理。

重复路径不意味着多份同一 Jar；Maven 选择一个版本不等于二进制兼容。移除单条依赖也不保证其他传递链没有 Web；实施时检查 effective-pom、完整 dependency:tree 和实际最小应用上下文。[Maven 机制依据](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html)

## 7. 数据结构与交付

以下是拟议结构变更，当前不生成或执行 SQL：

| 表/数据 | 改造 |
| --- | --- |
| flow_model | 增加 form_mode TINYINT，建议 0=FORM（FLOW 自带表单）、1=BUSINESS（宿主业务表单）；Java/接口字段 formMode |
| flow_definition | 增加 form_mode TINYINT 与 var_info JSON；Java/接口字段 formMode、varInfo，发布保存表单类型和变量定义快照 |
| flow_instance | 继续关联 definition_id、business_id 和已有审批数据；不重复存一套会与定义冲突的模式/字段结构 |
| formConfig | 保留独立表单的输入展示配置，不再作为执行层唯一字段契约 |

具体类型与约束建议：form_mode 为 TINYINT，Java 使用 Integer 并以枚举约束 0/1；新建请求明确提供表单类型，不隐式依据 formConfig 或 businessId 推断。模型设计态允许修改表单类型；仅保存设计态不影响当前生效定义或新申请，重新发布后，新申请才使用新类型。历史定义及其关联实例始终使用原 form_mode、form_config 和 var_info，不读取模型设计态类型来决定运行行为。发布定义对应字段必须完整；模式常量 0/1 属本次确认稿建议，尚未生成 SQL。

var_info 为 JSON，映射公开 FlowVariableSchema 类型，Java 属性 varInfo。建议结构为 fields 列表，每项包含 field、title、type、required，以及适用时的 elementType/format。type 沿用已有 FieldTypeEnum 数值，不另建字符串类型体系。无可用审批变量时保存明确的空 fields 列表，不用 null 表示跳过校验。表单条件性必填等规则继续由表单能力处理，不把复杂表单规则粗暴折算成全局 required。

var_info 描述审批可用变量，不取代表单配置。自带表单的子表单、附件等内容仍按表单规则保存与校验，不能因未参与条件判断就丢弃；业务模式按宿主声明的变量契约校验输入。若现有表单控件到变量类型存在无法表达的情况，先补具体映射方案并确认，不顺手增加新变量类型。

form_mode 的 SQL COMMENT 与实体注释一致；var_info COMMENT 说明其为发布时的审批变量定义快照。所有表和新增字段都带 COMMENT。完整基线从现有结构盘点生成，只包含 FLOW 所有的表和索引，不含 leave_* 表与演示数据。

交付目录：docs/sql/mysql/baseline、migrations、examples；示例脚本与生产基线隔离。新库执行本版本基线，已有库按起始版本执行增量；宿主现有迁移工具自行编排版本号，默认不在 Jar 内放入自动扫描的 schema.sql 或 db/migration，不新增 Flyway/Liquibase。

用户已确认现存历史均为 FLOW 自带表单；每个发布版本只使用一种表单类型，不同版本允许因设计态切换而采用不同类型。迁移时将存量 flow_model、flow_definition 的新增 form_mode 补为 FORM；从每个 flow_definition 自己的 form_config 提取并保存新增 var_info 元数据。这里只补新增结构字段，不修改实例 varMap 或申请内容。若表单快照格式需整理，先列出具体配置再适配，不引入历史业务契约恢复、模型拆分、legacy_mixed 或实例过渡方式字段。

已有运行实例不重新计算已完成路径；定义版本及事件记录保留。升级脚本不通过 IF NOT EXISTS 掩盖结构不一致，也不自动删除冲突数据。发布时附前置检查、适用版本与失败处理说明。业务库由宿主选择并执行，FLOW 不创建独立数据源。[初始化机制依据](https://docs.spring.io/spring-boot/3.4/how-to/data-initialization.html)

## 8. 前端与宿主联动

- api/flow 类型跟随后端公开契约更新，增加申请方式及变量快照；不只改界面不改类型。
- process/model/form 的编辑页按模式切换：业务模式显示宿主变量字段，自带表单模式转换表单字段；已发布版本查看使用快照。 保存设计态切换不改变当前发起目录；发布成功后目录和发起入口读取当前生效定义的类型，不直接使用模型草稿值。
- process-design 的条件字段、类型、运算符、引用检查消费变量定义；表单控件转换留适配位置，不要求条件编辑器理解表单组件。
- 发起目录按模式选择入口：独立表单进入 FLOW 填写，业务模式由宿主页面处理；服务端仍拒绝错误模式的发起请求。
- 以 approve/my/detail 为起点提取审批区域，参数建议 instanceId、可选 taskId，提供业务内容插槽/组合位置；审批完成后重新加载服务端状态，具体组件签名在前端实施清单核定。
- 宿主以申请记录关联加载对应业务详情，独立负责权限和脱敏；不得因可看 FLOW 记录就默认可看宿主单据。审批区域中的变量/表单数据仍由 FLOW 授权过滤。
- 示例同时展示业务单据事务接入和独立表单接入；系统设置服务仅在确有权限标识/组织适配接入变动时联动，不新增一套 FLOW 角色系统。

## 9. 兼容策略建议

| 项目 | 建议与影响 |
| --- | --- |
| FlowEngine | 没有外部调用方，全部内部示例与测试迁至职责接口后移除旧门面，不保留弃用委派层 |
| REST 坐标 | 直接改为 plasticene-flow-rest，同步内部依赖，不提供旧坐标 relocation 或旧实现 Jar |
| HTTP | 路径、现有响应包装和 varMap 等字段保留；新增模式/变量校验及统一授权会使此前错误或越权请求被拒绝，需列为升级行为变化 |
| 表单/模型/分类批量操作 | 保留原子性，全部资源都校验组织与授权，不逐条提交 |
| ProcessExecutor | 明确废止替换扩展，更新覆盖测试和使用说明，不偷偷失效 |
| SPI 旧无组织方法与事件策略 | 同步迁移内部示例后移除，不保留兼容方法或双策略路径 |
| DTO 与现有事件 | 保留字段语义、编号、已有事件顺序与幂等约定；类重定位及 Java 泛型变化同步改调用方和测试 |

无需维护外部兼容期；接口签名、严格变量编码及存量表单元数据提取规则仍属待评审实施细节。内部调用、测试和前端联动须在同一任务完成。

## 10. 分阶段改造与验收

| 阶段 | 关键位置与工作 | 行为影响/验收 |
| --- | --- | --- |
| 0 契约冻结 | 对照 FlowEngine、7 个 Controller、内部 Service，确认本稿方法映射、授权动作、变量编码、兼容和存量数据清单 | 每个现有 HTTP/Java 能力有归属；明确新增校验和有意废止项 |
| 1 公开 DTO | api/query/vo 及内部壳类型整理，事件运维 VO 公开化，同步泛型和映射 | JSON 字段不意外变化；类型编译通过，不丢分页或批量能力 |
| 2 统一用例 | impl/application、FlowEngineImpl、所有 Controller 和示例统一接入，注册授权 SPI | Java/HTTP 授权一致；表单修改删除补 actor；查询在用例内收紧范围；原有事务锁与批量原子性不变 |
| 3 模式与变量 | Model/Definition 参数、实体、Mapper、发布/发起/保存/预估校验与 SQL 草案，接入变量 SPI | 设计态切换未发布不影响发起；发布后新申请用新类型；历史实例保留原类型；错误模式拒绝及变量校验 |
| 4 前端联动 | api/flow、模型编辑、条件设计器、发起入口、详情审批区域、宿主示例 | 两种完整链路可用；发布快照回显；业务详情与 FLOW 数据授权分别生效 |
| 5 装配收口 | core/timeout/event 自动配置、imports、ProcessExecutor 与装配测试 | 无内核替换入口；超时开关/无通知/启停资源验证；纯 Java 不依赖 Web |
| 6 REST 交付 | 模块/POM/包名/示例/文档改名，移除平台 Web 直接依赖 | 标准宿主显式 Web 接入正常；新坐标与旧路径开关兼容；有效 POM/依赖树符合预期 |
| 7 数据库与回归 | 纯基线、增量、存量报告、部署指引；专用库验证 | 业务/实例/任务/事件同提交同回滚；版本迁移核验；既有并行、退回、转交、加签和事件交付回归 |

阶段之间按能力增量验证；公开类型、Controller 和调用方重定位应在一个可编译变更内完成，不留下旧新两套规则。SQL 部署必须先于要求新字段的应用上线。各仓库单独核对 git diff，不自动提交，新增文件按规则暂存。

后端运行受影响模块及依赖测试、自动配置最小上下文测试；跨模块/POM改造按根仓库要求扩大验证。前端运行相关 Vitest 与应用类型检查。真实数据库验证仅用专用验证库，外部服务未运行项明确报告。本轮仅执行文档差异检查，不声称实现测试已经通过。

## 11. 已澄清的存量事实与实施细节

用户已确认：没有外部真实 API 调用、没有服务引用该框架；现存历史都是 FLOW 自带表单；设计态允许切换表单类型，每个发布版本只采用一种类型，历史实例按原定义执行。因此取消外部兼容层、旧坐标迁移桥、历史业务契约恢复和混用模型过渡设计。

“回填”仅指为已有记录补新增字段：模型/定义的申请方式为 FORM，以及按每个发布定义的表单快照生成变量定义。既有实例继续通过 definitionId 获取对应快照，不改申请内容、审批记录和事件，不增加实例模式冗余字段。

后续只需按现有表单实际配置明确类型转换、日期/集合编码、系统字段与复杂控件处理。详见[旧接口迁移与存量表单元数据补齐](flow-migration-compatibility-plan.md)。这轮仍仅调整文档，不授权代码或数据库实施。

## 12. 最终实施约束

- 本确认稿整体通过后再改代码；若涉及未说明的实质变化或新增依赖，先补方案并单独确认。
- 先按职责建立唯一用例实现，随后同步所有 Controller、示例、测试与前端；删除旧 FlowEngine 门面，不保留双规则和外部兼容层。
- 具体类型化参数和方法清单以第 3、4 节为准；Java 使用 LoginUser actor，HTTP 从可信上下文获取。状态与资格在已有锁内重验，查询范围在用例内确定。
- 先适配现有表单字段和运算协议，再收紧变量校验；日期、集合、数字精度及复杂内容字段须以实际前后端样例验证，不借分层重构改变现有表达式语义。
- 数据库交付只有基线、增量和必要的一次性元数据转换工具；Starter 不执行 DDL，不连接或修改当前业务库。
- 保留现有 HTTP 路径、响应包装与配置开关；REST 坐标直接更名，由宿主显式引入 Web Starter。
- 后端和前端分别完成相关测试、类型检查及 diff 核对；数据库联调仅对专用验证库执行，当前环境未验证项明确列出。
- 新增文件按仓库规则暂存，不自动提交、推送或创建分支。

## 13. 表单类型切换规则（已确认）

- 模型设计态允许切换 FLOW 自带表单与宿主业务表单，不限制必须另建模型。
- 保存设计态时只更新草稿；当前生效定义、发起目录及新申请行为仍使用现有发布版本。
- 切到业务表单后，发布校验使用宿主变量契约；切到自带表单后，发布校验使用有效 FLOW 表单配置及其变量定义。条件字段引用、类型和字段权限须与目标配置一致；不自动改写条件语义以强行发布。
- 发布成功保存新 form_mode、form_config（适用时）和 var_info 快照，更新当前生效定义。新申请及对应页面入口随生效定义切换，指定 definitionId 的调用也须遵守当前可发起定义规则。
- 历史定义不修改；已有实例的办理、表单编辑、详情及变量校验继续以其 definitionId 对应的原快照为准。
- 同一模型不同版本类型不同属于正常版本演进，不属于同一发布定义混用，不增加任何 legacy 运行分支。
- 测试覆盖 FORM→BUSINESS 和 BUSINESS→FORM：未发布、发布后新发起、旧实例继续办理及历史版本回显四个阶段。
