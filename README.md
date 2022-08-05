# plasticene-boot-starter-parent

## 1.项目介绍

- **基于SpringBoot2.x、SpringCloud和SpringCloudAlibaba企业级系统架构底层框架封装**。
- 解决业务开发时常见的**非功能性需求**，防止重复造轮子，方便业务快速开发和企业技术栈框架统一管理。
- **引入组件化的思想实现高内聚低耦合并且高度可配置化，做到可插拔**。
- 严格控制包依赖和统一版本管理，做到**最少化依赖**。
- 注重代码规范和注释，非常适合个人学习和企业使用。

**实现功能概述：**

- 整合redis+caffeine实现**多级缓存**
- 基于mybatis-plus封装实现**分页、多租户插件，公共属性自动填充，复杂字段类型处理，字段数据加密**，逻辑删除等。
- 基于redis实现**分布式限流注解**，基于Redisson实现**分布式锁注解**，以及对redis常用命令和组件的封装
- 实现**license证书生成与检验**，只需引入依赖，**开箱即用**。
- 实现web设置接口traceId实现**链路追踪**，**接口访问日志打印**，**全局异常和响应结果统一处理封装**，整合接口文档knife框架等
- 自定义启动banner图案和控制台打印
- 基于**TransmittableThreadLocal封装线程池**实现父子线程之间的数据传递
- 封装公共pojo类、业务异常类、工具类

**开发文档**：https://github.com/plasticene/plasticene-boot-starter-parent/wiki

项目地址：

**Github地址**：https://github.com/plasticene/plasticene-boot-starter-parent

**Gitee地址**：https://gitee.com/plasticene3/plasticene-boot-starter-parent

## 2.项目结构

**模块结构**

```lua
plasticene-boot-starter-parent -- 父项目
│  ├─plasticene-boot-starter-banner -- 图案
│  │─plasticene-boot-starter-cache -- 多级缓存
│  ├─plasticene-boot-starter-mybatis -- mybatis starter
│  ├─plasticene-boot-starter-redis -- redis starter
│  ├─plasticene-boot-starter-web -- web starter
│  ├─plasticene-boot-starter-license -- license starter
│  ├─plasticene-common -- 公共、基础
```

**代码结构：**

每个starter组件，包含两部分：

1. `core` 包：组件的核心封装，拓展相关的功能。
2. `autoconfigure` 包：组件的 Spring Boot 自动配置。

<img src="https://markdown-file-zfj.oss-cn-hangzhou.aliyuncs.com/%E4%BB%A3%E7%A0%81%E7%BB%93%E6%9E%84.png" style="zoom:50%;" />

**模块说明**

|      | 模块                                                         | 功能                                                         |                             文档                             |
| :--: | ------------------------------------------------------------ | ------------------------------------------------------------ | :----------------------------------------------------------: |
|  🚀   | [plasticene-boot-starter-parent](https://github.com/plasticene/plasticene-boot-starter-parent) | 父项目，统一依赖版本管理                                     | [plasticene-parent](https://github.com/plasticene/plasticene-boot-starter-parent/wiki) |
|  🚀   | [plasticene-boot-starter-banner](https://github.com/plasticene/plasticene-boot-starter-parent/tree/main/plasticene-boot-starter-banner) | 自定义项目启动图案，配置控制台打印相关信息等等               |                                                              |
|  🚀   | [plasticene-boot-starter-cache](https://github.com/plasticene/plasticene-boot-starter-parent/tree/main/plasticene-boot-starter-cache) | 基于spring cache实现多级缓存                                 | [cache](https://github.com/plasticene/plasticene-boot-starter-parent/wiki/multilevel-cache(%E5%A4%9A%E7%BA%A7%E7%BC%93%E5%AD%98)) |
|  🚀   | [plasticene-boot-starter-mybatis](https://github.com/plasticene/plasticene-boot-starter-parent/tree/main/plasticene-boot-starter-mybatis) | 基于mybatis-plus进行二次封装整合                             | [mybatis](https://github.com/plasticene/plasticene-boot-starter-parent/wiki/mybatis%E4%BA%8C%E6%AC%A1%E5%B0%81%E8%A3%85starter) |
|  🚀   | [plasticene-boot-starter-redis](https://github.com/plasticene/plasticene-boot-starter-parent/tree/main/plasticene-boot-starter-redis) | 实现常规redis操作封装，基于redis实现分布式限流，基于redisson实现分布式锁 | [redis](https://github.com/plasticene/plasticene-boot-starter-parent/wiki/plasticene-boot-starter-redis) |
|  🚀   | [plasticene-boot-starter-web](https://github.com/plasticene/plasticene-boot-starter-parent/tree/main/plasticene-boot-starter-web) | 实现链路追踪traceId、接口请求日志打印、统计返回结构封装、全局异常处理、swagger配置等 | [web](https://github.com/plasticene/plasticene-boot-starter-parent/wiki/plasticene-boot-starter-web) |
|  🚀   | [plasticene-boot-starter-license](https://github.com/plasticene/plasticene-boot-starter-parent/tree/main/plasticene-boot-starter-license) | license版权证书生成与验证                                    | [license](https://github.com/plasticene/plasticene-boot-starter-parent/wiki/plasticene-boot-starter-license) |
|  🚀   | [plasticene-common](https://github.com/plasticene/plasticene-boot-starter-parent/tree/main/plasticene-common) | 定义公共pojo类、业务异常类、工具类，枚举，线程池等等         |                                                              |



## 3.技术选型

| 框架                                                         | 说明                  | 版本        |
| ------------------------------------------------------------ | --------------------- | ----------- |
| [Spring Boot](https://spring.io/projects/spring-boot)        | 应用开发框架          | 2.7.0       |
| [Spring Cloud](https://spring.io/projects/spring-cloud)      | 微服务开发框架        | 2021.0.3    |
| [Spring Cloud Alibaba](https://spring.io/projects/spring-cloud-alibaba) | 微服务开发框架        | 2021.0.1.0  |
| [Openfeign](https://spring.io/projects/spring-cloud-openfeign) | 声明式 REST 客户端    | 3.1.3       |
| [MySQL](https://www.mysql.com/cn/)                           | 数据库服务器          | 5.7         |
| [Druid](https://github.com/alibaba/druid)                    | JDBC 连接池、监控组件 | 1.2.8       |
| [MyBatis Plus](https://mp.baomidou.com/)                     | MyBatis 增强工具包    | 3.5.2       |
| [Redis](https://redis.io/)                                   | key-value 数据库      | 5.0         |
| [Redisson](https://github.com/redisson/redisson)             | Redis 客户端          | 3.17.4      |
| [Spring MVC](https://github.com/spring-projects/spring-framework/tree/master/spring-webmvc) | MVC 框架              | 5.3.20      |
| [Hibernate Validator](https://github.com/hibernate/hibernate-validator) | 参数校验组件          | 6.1.5.Final |
| [Knife4j](https://gitee.com/xiaoym/knife4j)                  | Swagger 增强 UI 实现  | 3.0.2       |
| [Jackson](https://github.com/FasterXML/jackson)              | JSON 工具库           | 2.11.2      |
| [Lombok](https://projectlombok.org/)                         | 消除冗长的 Java 代码  | 1.16.14     |
| [JUnit](https://junit.org/junit5/)                           | Java 单元测试框架     | 5.8.2       |
| [Mockito](https://github.com/mockito/mockito)                | Java Mock 框架        | 4.0.0       |
| [Hutool](https://www.hutool.cn/docs/#/)                      | 常用工具类框架        | 5.7.20      |
| [transmittable-thread-local](https://github.com/alibaba/transmittable-thread-local) | 线程池异步上下文传递  | 2.12.2      |
| [caffeine](https://github.com/ben-manes/caffeine)            | 高性能本地缓存之王    | 2.8.5       |
| [slf4j](https://www.slf4j.org/)                              | 日志框架              | 1.7.36      |
| [aspectj](https://www.eclipse.org/aspectj/)                  | 切面框架              | 1.9.6       |
| [truelicense](https://truelicense.namespace.global/)         | license证书管理引擎   | 1.33        |
| [jdk](https://github.com/openjdk/jdk)                        | Java 开发工具包       | >=1.8       |
| [maven](https://maven.apache.org/)                           | Java 管理与构建工具   | >=3.5.0     |

## 4.使用示例

当前组件依赖暂时还未上传到maven中央仓库，所以需要自行克隆代码到本地install

- git clone https://github.com/plasticene/plasticene-boot-starter-parent.git
- cd plasticene-boot-starter-parent && mvn install
- have fun and enjoy.

在业务团队项目服务的工程中按下面引入`plasticene-boot-starter-parent`相关依赖即可：

```xml
   <!-- 使用plasticene-boot-starter-parent代替spring boot官方parent，统一依赖版本管理 -->
   <parent>
        <artifactId>plasticene-boot-starter-parent</artifactId>
        <groupId>com.plasticene.boot</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    
    <!-- 引入相关依赖 -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.plasticene.boot</groupId>
                <artifactId>plasticene-boot-starter-banner</artifactId>
                <version>1.0-SNAPSHOT</version>
            </dependency>

            <dependency>
                <groupId>com.plasticene.boot</groupId>
                <artifactId>plasticene-boot-starter-mybatis</artifactId>
                <version>1.0-SNAPSHOT</version>
            </dependency>

            <dependency>
                <groupId>com.plasticene.boot</groupId>
                <artifactId>plasticene-boot-starter-web</artifactId>
                <version>1.0-SNAPSHOT</version>
            </dependency>

            <dependency>
                <groupId>com.plasticene.boot</groupId>
                <artifactId>plasticene-boot-starter-redis</artifactId>
                <version>1.0-SNAPSHOT</version>
            </dependency>

            <dependency>
                <groupId>com.plasticene.boot</groupId>
                <artifactId>plasticene-boot-starter-cache</artifactId>
                <version>1.0-SNAPSHOT</version>
            </dependency>

        </dependencies>
    </dependencyManagement>

```

完整项目服务整合代码示例请看：https://github.com/plasticene/fast-api

## 5. 交流反馈

- 如果有问题或者其他交流，可以通过 [Github Issue](https://github.com/plasticene/plasticene-boot-starter-parent/issues)或者 [Gitee Issue](https://gitee.com/plasticene3/plasticene-boot-starter-parent/issues)进行反馈。提交`ISSUS`时，请务必写清楚问题的具体原因，重现步骤和环境(上下文)，以便作者后期重现排查解决。

- 个人博客：http://www.shepherd126.top/

- 个人邮箱：shepherd_zfj@163.com

- 个人公众号：[Shepherd进阶笔记](https://camo.githubusercontent.com/1275dd8e8b4118823c0f8976f653945eafe77708877e832c59f9a4d9e9d31180/68747470733a2f2f6d61726b646f776e2d66696c652d7a666a2e6f73732d636e2d68616e677a686f752e616c6979756e63732e636f6d2f4f6666696369616c2532304163636f756e742e6a7067)

  