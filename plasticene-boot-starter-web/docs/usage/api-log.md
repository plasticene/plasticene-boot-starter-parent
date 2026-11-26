# 接口日志

宿主引入 plasticene-boot-starter-web 后，通过以下配置开启接口日志：

```yaml
ptc:
  api:
    log:
      enable: true
```

总开关默认关闭。开启后，现有切点 `com.plasticene..controller..` 覆盖的 Controller 方法默认记录请求参数、返回结果和正常返回耗时，不再要求标注注解；仅设置日志级别不会开启该功能。

不需要记录日志的接口，在方法或 Controller 类上添加 `com.plasticene.boot.web.core.anno.ApiLogIgnore`。类上的忽略作用于整个 Controller，方法上的忽略只作用于该方法；忽略注解优先于默认记录。代理实现类、接口方法及继承层次中的忽略标记也会被识别。

@ApiLog 已删除，使用方应移除旧注解和 import。需要跳过的接口改用 @ApiLogIgnore，不能直接把所有旧注解替换成忽略注解。总开关开启后，未标记的接口也会记录日志；登录、令牌和密码相关接口应排除。

FLOW REST 无需依赖 Web Starter 或增加注解，由引入 Web Starter 的宿主统一打印日志。@ApiLogIgnore 属于 Web Starter，需要使用忽略标记的模块应具有该依赖。
