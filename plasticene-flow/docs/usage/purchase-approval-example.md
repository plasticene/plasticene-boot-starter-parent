# 采购业务审批示例

示例宿主是同级仓库 `plasticene-admin`，前端入口为“演示 → 业务审批示例”。采购业务代码位于 `com.plasticene.admin.purchase`，只新增一张 `purchase_apply` 表，不向 FLOW 引擎模块添加采购逻辑。

## 初始化

1. 在 admin 的本地开发数据库中执行 `plasticene-admin/docs/sql/purchase/purchase-apply.sql`。FLOW 初始化和旧库迁移见 admin 的 `docs/deployment/flow-integration.md`。保留 FLOW 自身已有的模型、实例、任务及事件表；新业务表须与 FLOW 使用同一数据源、同一事务管理器。
2. 启动 admin 的 `PlasticeneAdminApplication`，无需启动 FLOW example。使用本地开发环境的数据库、Redis 和登录认证配置，启动端口沿用现有配置。
3. 在流程分类管理中新建编码为 `purchase` 的分类。
4. 新建流程模型：编码 `purchase-approval`，分类 `purchase`，表单模式选“业务表单”。选择有权限发起该模型的用户范围，配置一个人工审批节点，选择实际可登录的审批人，然后发布并启用模型。
5. 如需测试条件分支，可以使用 `amount`（数字）、`category`（字符串）和 `urgent`（数字，0 否、1 是）。变量契约由示例宿主的 `PurchaseFlowConfiguration` 提供，无需在流程中设计采购表单。
6. 启动前端，进入“演示 → 业务审批示例”。填写七个字段，点击“提交审批”。

流程分类用于 `FlowEventConsumer.businessType()` 路由回调，必须使用 `purchase`。若分类不正确，提交接口会回滚采购单和本次流程创建，提示修正模型后重新发布。未发布或未启用的流程沿用 FLOW 自身校验错误。

## 业务闭环

- 提交调用 `POST /flow/demo/purchase/submit`，宿主生成可信操作者，从已保存的采购单构造变量，调用 `FlowInstanceApi.startInstanceByCode`。保存、启动、回写实例 ID 在同一事务中完成。
- 流程编码固定为 `purchase-approval`，业务 ID 为 `purchase:{采购申请ID}`。
- 浏览器为每次新申请生成 `requestNo`，同一表单的失败重试保持不变。数据库以组织和请求标识唯一约束处理并发重复请求，返回已关联的原实例。
- `GET /flow/demo/purchase/page` 查询当前登录用户的申请；`GET /flow/demo/purchase/{id}` 校验组织及 FLOW 实例查看权限，供发起人和审批人查看。
- 采购表中的 `status` 由异步事件回调更新：0 审批中、1 已通过、2 已拒绝、3 已取消。列表不会使用实时流程状态覆盖它，因此可以观察流程完成到业务回调生效之间的短暂延迟；点击列表刷新查看最新状态。
- 从列表查看详情或从“我的待办”进入时，左侧显示采购单，右侧沿用审批路径；业务表单隐藏 FLOW 表单变更记录。

## 回调和模拟通知

`PurchaseFlowEventConsumer` 使用现有 FLOW 事件投递机制，处理 `INSTANCE_APPROVED`、`INSTANCE_REJECTED` 和 `INSTANCE_CANCELLED`。开始事件无需额外写入，采购单初始即为审批中；任务级事件不触发采购业务处理。

回调按组织、业务 ID 和实例 ID 校验关联并加行锁，只允许审批中进入终态；重复或旧事件不会再次更新终态。业务处理异常继续抛出，交由 FLOW 已有事件投递机制重试。

只有整个流程审批通过，且采购业务状态成功从审批中更新为已通过时，才注册事务提交后的日志输出：

```text
【模拟通知行政】采购申请 7「办公电脑采购」已审批通过，金额 5000.00 元，请安排下单采购。
```

这是日志模拟，不接入真实消息渠道，不保存通知字段或通知表。事务回滚时不输出，重复回调不重复输出；进程若在提交后、输出前退出，日志可能缺失，不作为可靠消息送达机制。

## 验证

前端仓库运行：

```bash
pnpm vitest run --dom apps/web-antd/src/views/demos/business-approval apps/web-antd/src/views/flow/approve/my/detail/index.test.ts
pnpm -F @vben/web-antd typecheck
```

在 `plasticene-admin` 仓库运行（本地 Maven 仓库应已有当前 FLOW 模块及其依赖）：

```bash
mvn -Dtest=PurchaseApplyServiceTest,PurchaseFlowEventConsumerTest,PurchaseFlowConfigurationTest test
```

这些测试无需访问业务数据库，覆盖可信变量、校验、重复提交、查看权限、回调重复处理、失败后重试及事务提交/回滚时的日志行为。真实 MySQL 单表持久化、并发锁与完整事件投递仍须在本地开发环境联调：提交后由审批人办理待办，刷新采购列表确认终态，并核对审批通过后的模拟通知日志。拒绝、取消应更新对应状态且不输出行政通知。
