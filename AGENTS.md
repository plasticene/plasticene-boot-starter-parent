# AGENTS.md

## 项目概览

本仓库是一个 Java 21、Spring Boot 3.4.x 的 Maven 多模块项目，用于提供可独立引入的基础库和 Spring Boot Starter。根目录 `pom.xml` 是版本与依赖管理的唯一权威来源；README 中的部分版本信息可能滞后。

主要模块：

- `plasticene-common`：公共 POJO、异常、工具类、线程与用户上下文等基础能力。
- `plasticene-boot-starter-web`：Web 自动配置、统一响应、异常处理、链路追踪、接口安全和校验。
- `plasticene-boot-starter-mybatis`：MyBatis-Plus 扩展、多租户、字段处理、代码生成器与模板。
- `plasticene-boot-starter-redis`：Redis 工具、分布式锁、限流和消息监听。
- `plasticene-boot-starter-cache`：Caffeine + Redis 多级缓存。
- `plasticene-boot-starter-flow`：流程模型、运行时、任务、表单及其持久层。
- `plasticene-boot-starter-delay-queue`：基于 Redis 的分布式延迟队列。
- `plasticene-boot-starter-banner`、`license`、`oss`、`data-scope`、`excel`：对应的可插拔 Starter。
- `plasticene-boot-example`：示例应用聚合项目；当前未加入根项目 reactor，需单独构建。

## 开发约定

- 保持模块边界，优先在功能所属模块内修改；不要为了复用而让底层模块反向依赖上层 Starter。
- Java 包名保持 `com.plasticene.boot...`，使用 4 空格缩进并遵循现有类的命名、注释和大括号风格。
- 项目使用 Java 21 和 Jakarta API。新增代码不要重新引入 `javax.*` 或降低语言/字节码版本。
- 可以沿用项目已有的 Lombok 用法，但不要仅为局部代码引入新的依赖或新的代码风格。
- 修改公共类、注解、配置属性或 Bean 名称时，将其视为对使用方的公开 API 变更；优先保持源码和配置兼容。
- 新增依赖时，版本统一放在根 `pom.xml` 的 properties/dependencyManagement 中；子模块只声明实际需要的依赖，避免扩大传递依赖。
- 修改 MyBatis DAO、实体或查询结构时，同步检查对应的 `src/main/resources/mapper/*.xml`。
- 修改代码生成器时，同步检查 `plasticene-boot-starter-mybatis/src/main/resources/templates/codegen/` 下的模板。
- 在任意模块的 `src` 目录下新增目录后，必须执行 `git add <新增目录路径>`，将该目录中的新增文件加入暂存区；Git 不跟踪空目录，因此新增目录必须包含应提交的文件。
- `src` 目录下新增 Java 文件时，必须在顶级类、接口、枚举或记录声明前添加包含功能说明、作者和创建日期的 Javadoc。作者固定为 `ZFJ`，`@since` 使用创建当天日期，格式为 `yyyy-MM-dd`；功能说明应按文件实际职责填写。例如：

  ```java
  /**
   * 用户service
   *
   * @author ZFJ
   * @since 2026-09-03
   */
  ```

- 不要提交或手工修改构建/IDE 产物，包括 `target/`、`.flattened-pom.xml`、`*.iml`、`.idea/`、日志和 JVM 崩溃日志。

## Spring Boot Starter 约定

- Starter 的自动配置放在 `autoconfigure` 包，核心实现放在 `core` 包。
- Spring Boot 3 自动配置优先登记在：
  `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- 新增、移动或重命名自动配置类时，必须同步更新上述 imports 文件，并验证该类能由最小应用上下文加载。
- `spring.factories` 仅用于仍由 Spring 支持的扩展点或现有兼容代码；不要用它新增 Boot 3 的 `EnableAutoConfiguration` 注册。
- 面向使用方的 Bean 尽量使用 `@ConditionalOnMissingBean`，可选功能使用 `@ConditionalOnProperty`，避免 Starter 无条件覆盖业务应用自定义配置。
- 新增 `@ConfigurationProperties` 时保持现有 `ptc.*` 前缀体系，并确保已被自动配置启用；配置项变更应同步默认属性和模块文档。

## 构建与验证

仓库没有 Maven Wrapper，使用本机 `mvn`。在仓库根目录执行：

```bash
# 快速检查 POM 与 reactor
mvn validate

# 验证某个模块及其依赖模块（首选的日常验证方式）
mvn -pl plasticene-boot-starter-web -am test

# 验证根 reactor 中的全部模块
mvn test

# 安装到本地仓库，供未加入 reactor 的示例项目使用
mvn install

# 单独验证示例项目（通常先执行上面的 install）
mvn -f plasticene-boot-example/pom.xml test
```

验证要求：

- 小改动至少运行受影响模块的 `mvn -pl <module> -am test`。
- 跨模块、父 POM、依赖版本或自动配置注册变更应运行根目录 `mvn test`；准备发布时运行 `mvn install`。
- 涉及 Redis、数据库、OSS、License 等外部设施的测试，先确认测试配置不会连接或修改真实环境；无法自动验证时，在交付说明中列出未验证项和原因。
- 当前核心 Starter 的自动化测试覆盖有限。修复缺陷或增加非简单逻辑时，应在所属模块补充聚焦测试；不要只依赖 example 中的 `@SpringBootTest`。
- Maven 的 flatten 插件会在构建阶段生成 `.flattened-pom.xml`；它们是生成物，不是源文件。

## 修改流程

1. 修改前检查 `git status --short`，保留并避开用户已有的未提交改动。
2. 阅读目标模块的 `pom.xml`、相邻实现、自动配置注册文件和相关文档，再做最小范围修改。
3. 对行为变化补充或更新测试；对配置/API 变化同步文档与示例。
4. 运行与改动范围匹配的 Maven 验证，并检查 `git diff`，确保没有混入生成文件或无关格式化。
5. 交付时说明修改文件、行为影响、已运行的验证，以及任何依赖外部服务而未运行的验证。

## 安全注意事项

- 示例配置中的数据库、Redis、OSS、密钥、证书和接口安全参数均按敏感信息处理，不提交真实凭据。
- 不在日志中输出密码、令牌、签名私钥、License 私钥或完整敏感请求体。
- 修改加密、签名、脱敏、租户隔离、限流或分布式锁逻辑时，必须考虑失败路径、并发行为和向后兼容，并增加针对性验证。
