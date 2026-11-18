# 业务审批接入

当前接入契约见 [模块接入](flow-modules.md)。旧 FlowEngine 已删除，业务发起使用 FlowInstanceApi。

```java
var param = new FlowStartByCodeParam();
param.setProcessCode("leave-approval");
param.setBusinessId(applicationId);
param.setVarMap(Map.of("days", 2, "reason", "请假"));
Long instanceId = instances.startInstanceByCode(param, actor);
```

宿主先校验并保存业务申请，在相同 DataSource/事务管理器下调用并回写 instanceId。发起异常向外传播，业务、实例、任务和事件一起回滚。提供授权策略、组织人员适配及业务事件消费者；仅在流程使用业务字段时提供变量契约，无变量流程默认使用空契约。详见 plasticene-boot-flow-example 的 leave 示例。

同一申请标识重复发起会报错，不承诺返回旧实例。流程结束和业务生效通过可靠事件解耦，事件至少一次投递，业务接收方自行幂等。业务处理失败不回滚已提交的审批。

现存自带表单升级时仅补 form_mode 和各定义的 var_info，不修改实例申请内容。部署顺序见 [SQL说明](../sql/mysql/README.md)。

业务流程发布前会比较草稿保存的 varInfo 与宿主最新变量定义。字段新增、删除或定义属性变化时阻止发布，须先刷新变量、适配工作流配置并保存；仅字段排列顺序变化不影响发布。发布快照采用校验通过的草稿定义，已有发布版本及实例继续使用各自快照。宿主和草稿均无变量时仍可直接发布。
