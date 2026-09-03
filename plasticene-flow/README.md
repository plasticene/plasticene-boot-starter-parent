# Plasticene Flow

Plasticene Flow 是面向业务审批场景的轻量级嵌入式流程引擎。业务应用引入一个 Starter，复用自己的数据源即可使用流程能力。

## 模块

- `plasticene-flow-core`：公开 API、命令、流程模型、SPI，以及发布、启动、审批、驳回、流转和幂等实现。领域契约不依赖 MyBatis、Web DTO 或登录上下文。
- `plasticene-flow-mybatis`：MyBatis 数据对象、Mapper 和 `FlowRepository` 实现。
- `plasticene-boot-starter-flow`：Spring Boot 自动配置、数据库迁移以及 MySQL/PostgreSQL 驱动适配。

依赖方向固定为：`starter -> core + mybatis`，`mybatis -> core`。业务代码通常只面向 `FlowEngine` 和 `FlowAssigneeProvider`。

## 引入

```xml
<dependency>
    <groupId>com.plasticene.boot</groupId>
    <artifactId>plasticene-boot-starter-flow</artifactId>
    <version>${plasticene.version}</version>
</dependency>
```

Starter 在存在 `DataSource` 时生效。数据库结构由独立的 Flyway 历史表 `ptc_flow_schema_history` 管理：

```yaml
ptc:
  flow:
    enabled: true
    database:
      schema-action: validate
    outbox:
      enabled: true
      initial-delay: 5s
      poll-interval: 2s
      retry-interval: 30s
      batch-size: 50
      max-attempts: 10
    web:
      enabled: false
```

`schema-action` 支持：

- `validate`（默认）：只校验结构和待执行迁移，不修改数据库。
- `update`：执行尚未应用的迁移，适合本地开发和受控部署。
- `create`：仅在尚未应用 Flow 迁移时建表。
- `create-drop`：启动时重建、关闭时删除，仅用于测试。
- `none`：完全关闭结构管理。

应用通过注册自己的 `FlowAssigneeProvider` 连接组织架构；没有自定义实现时，使用模型节点内配置的审批人。租户和操作者由每条命令的 `FlowActor` 显式传入，核心层不会读取 `LoginUserHolder`。为了兼容原有表中的 `org_id`、`user_id`、`assignee` 和 `business_id`，默认 MyBatis 实现要求对应字符串能够转换为 `Long`。

## 可选 HTTP 接口

Starter 默认不注册 Controller，只向应用提供 `FlowEngine` 等嵌入式能力。Servlet Web 应用可以显式开启：

```yaml
ptc:
  flow:
    web:
      enabled: true
```

开启后注册 `/flow/**`，包括流程模型和表单保存、模型发布、流程发起、实例和任务查询，以及审批、驳回、退回、转办、撤回和取消接口。所有写接口都必须传递 `Idempotency-Key` 请求头。

默认的 `HeaderFlowWebActorResolver` 从以下请求头构造 `FlowActor`：

- `X-Flow-Tenant-Id`：租户标识，必填。
- `X-Flow-Operator-Id`：当前操作人标识，必填。
- `X-Flow-Roles`：逗号分隔的角色标识，可选。

请求头名称可以通过 `ptc.flow.web.tenant-header`、`operator-header` 和 `roles-header` 修改。生产环境不能直接信任来自公网的身份请求头，应由认证网关覆盖这些请求头，或者由业务应用注册自己的 `FlowWebActorResolver`，从现有安全上下文解析身份。HTTP 相关依赖声明为 optional，因此启用该功能的应用需要已经具备 Spring MVC 环境。

## 查询与任务操作

`FlowEngine` 同时提供有界分页查询：流程模型与版本、我的发起、我的任务以及流程实例详情。实例详情包含流程定义、表单实例和完整任务轨迹。分页 `limit` 范围为 1～200。

除审批和驳回外，运行中任务支持转交、退回已经经过的审批节点；申请人可以在任务尚未处理时撤回，也可以取消自己发起的运行中流程。所有写命令继续使用 `requestId` 保证幂等，并在锁定流程实例后更新状态。

## 条件分支

`CONDITION` 节点通过 `FlowBranch` 配置多个候选分支和最多一个默认分支。`FlowCondition` 支持条件组之间以及组内规则的 `AND`/`OR` 组合，内置比较操作包括 `EQ`、`NE`、`GT`、`GE`、`LT`、`LE`、`IN`、`NOT_IN`、`CONTAINS`、`EMPTY` 和 `NOT_EMPTY`。条件只读取流程变量，不执行 SpEL 或任意脚本。

## 事件投递

流程事件先与业务状态一起写入 `flow_outbox_event`，提交后由 Starter 后台线程以至少一次语义投递。默认通过 Spring `ApplicationEventPublisher` 发布；业务项目也可以注册自己的 `FlowEventHandler`。失败事件按照 `retry-interval` 重试，达到 `max-attempts` 后保留在表中等待人工处理。

## 数据库兼容

原有的 `category`、`form`、`flow_model`、`flow_definition`、`flow_instance` 和 `flow_task` 保持原表名与原字段，不使用 `ptc_flow_*` 重命名，也不向这些表追加新架构字段。模型、实例和任务状态继续使用原来的整数编码；取消、退回等新增状态只扩展新的整数值。

`flow_form_instance`、`flow_command` 和 `flow_outbox_event` 是本次为表单提交快照、命令幂等和可靠事件投递增加的新表。已经存在旧业务表但尚无 Flow Flyway 历史表时，`update` 模式会从版本 0 建立基线并只补充缺失表。

## 当前能力边界

当前实现支持 `START`、`APPROVAL`、`COPY`、`CONDITION`、`END` 节点，会签（`ALL`）、或签（`ANY`）、顺签（`ORDER`），以及审批、驳回、退回、转交、撤回和取消。流程模型、定义、表单和运行数据均按租户隔离。

当前定位是嵌入式 Starter，可按需开启 REST API；不包含独立 Server、远程 Client、任意脚本表达式和 Webhook。
