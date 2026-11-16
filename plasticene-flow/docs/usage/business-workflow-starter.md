# 业务审批接入

当前接入契约见 [模块接入](flow-modules.md)。旧 FlowEngine 已删除，业务发起使用 FlowInstanceOperations。

```java
var param = new FlowBusinessStartByCodeParam();
param.setProcessCode("leave-approval");
param.setBusinessId(applicationId);
param.setVarMap(Map.of("days", 2, "reason", "请假"));
Long instanceId = instances.startBusinessByCode(param, actor);
```

宿主先校验并保存业务申请，在相同 DataSource/事务管理器下调用并回写 instanceId。发起异常向外传播，业务、实例、任务和事件一起回滚。提供授权策略、组织人员适配、业务变量契约及业务事件消费者。详见 plasticene-boot-flow-example 的 leave 示例。

同一申请标识重复发起会报错，不承诺返回旧实例。流程结束和业务生效通过可靠事件解耦，事件至少一次投递，业务接收方自行幂等。业务处理失败不回滚已提交的审批。

现存自带表单升级时仅补 form_mode 和各定义的 var_info，不修改实例申请内容。部署顺序见 [SQL说明](../sql/mysql/README.md)。
