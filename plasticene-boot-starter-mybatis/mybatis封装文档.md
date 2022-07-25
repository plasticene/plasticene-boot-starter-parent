# plasticene-boot-starter-mybatis

`plasticene-boot-starter-mybatis` starter组件是基于mybatis-plus进行二次封装，实现对数据库的操作的。如果没有使用mybatis-plus框架，可以先到[ mybatis-plus官网](https://baomidou.com/pages/24112f/)入门一波。

### 1.分页插件注入

```java
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor()); // 分页插件
        return mybatisPlusInterceptor;
    }
```

### 2.实体类公共属性自动填充

在业务开发表设计过程中，每张表都有公共字段创建时间、更新时间、创建人、更新人....等等。我们把这些字段抽取到实体基类，方便统一处理。

[BaseDO ](https://github.com/plasticene/plasticene-boot-starter-parent/blob/main/plasticene-boot-starter-mybatis/src/main/java/com/plasticene/boot/mybatis/core/metadata/BaseDO.java)是所有数据库实体的**父类**

```java
@Data
public class BaseDO implements Serializable {
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    /**
     * 最后更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
    private Long creator;
    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updater;
    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;
}
```

[DefaultDBFieldHandler](https://github.com/plasticene/plasticene-boot-starter-parent/blob/main/plasticene-boot-starter-mybatis/src/main/java/com/plasticene/boot/mybatis/core/handlers/DefaultDBFieldHandler.java)对公共字段属性进行自动填充：

```java
/**
 * 公共字段属性值自动填充
 */
public class DefaultDBFieldHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        if (Objects.nonNull(metaObject) && metaObject.getOriginalObject() instanceof BaseDO) {
            BaseDO baseDO = (BaseDO) metaObject.getOriginalObject();

            Date current = new Date();
            // 创建时间为空，则以当前时间为插入时间
            if (Objects.isNull(baseDO.getCreateTime())) {
                baseDO.setCreateTime(current);
            }
            // 更新时间为空，则以当前时间为更新时间
            if (Objects.isNull(baseDO.getUpdateTime())) {
                baseDO.setUpdateTime(current);
            }

            //todo 完善获取登录用户信息
//            LoginUser currentUser = RequestUserHolder.getCurrentUser();
            Long userId = 1l;
            // 当前登录用户不为空，创建人为空，则当前登录用户为创建人
            if (Objects.nonNull(userId) && Objects.isNull(baseDO.getCreator())) {
                baseDO.setCreator(userId);
            }
            // 当前登录用户不为空，更新人为空，则当前登录用户为更新人
            if (Objects.nonNull(userId) && Objects.isNull(baseDO.getUpdater())) {
                baseDO.setUpdater(userId);
            }
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时间为空，则以当前时间为更新时间
        Object modifyTime = getFieldValByName("updateTime", metaObject);
        if (Objects.isNull(modifyTime)) {
            setFieldValByName("updateTime", new Date(), metaObject);
        }

//        LoginUser currentUser = RequestUserHolder.getCurrentUser();
        Long userId = 1l;
        // 当前登录用户不为空，更新人为空，则当前登录用户为更新人
        Object modifier = getFieldValByName("updater", metaObject);
        if (Objects.nonNull(userId) && Objects.isNull(modifier)) {
            setFieldValByName("updater", userId, metaObject);
        }
    }
}
```

### 3.复杂字段类型处理

MyBatis Plus 提供 TypeHandler 字段类型处理器，用于 JavaType 与 JdbcType 之间的转换：

```java
@Data
@TableName(autoResultMap = true)
public class DataSource extends BaseDO implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String host;
    private String port;
    @TableField(typeHandler = JsonStringSetTypeHandler.class)
    private Set<String> databaseList;
    private String userName;
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String password;
    private Integer type;
}
```

这里常用的类型转换器供业务侧使用：

[JacksonTypeHandler](https://github.com/baomidou/mybatis-plus/blob/a3e121c27cd26cb7c546dfb88190f3b1f574dc38/mybatis-plus-extension/src/main/java/com/baomidou/mybatisplus/extension/handlers/JacksonTypeHandler.java): 通用的 Jackson 实现 JSON 字段类型处理器

[JsonStringSetTypeHandler](https://github.com/plasticene/plasticene-boot-starter-parent/blob/main/plasticene-boot-starter-mybatis/src/main/java/com/plasticene/boot/mybatis/core/handlers/JsonStringSetTypeHandler.java)：Set<String> 的类型转换器

### 4.BaseMapperX

[BaseMapperX](https://github.com/plasticene/plasticene-boot-starter-parent/blob/main/plasticene-boot-starter-mybatis/src/main/java/com/plasticene/boot/mybatis/core/mapper/BaseMapperX.java)接口，继承 MyBatis Plus 的 BaseMapper 接口，提供更强的 CRUD 操作能力。

将`selectPage`分页查询封装返回自定义的数据结构[PageResult](https://github.com/plasticene/plasticene-boot-starter-parent/blob/main/plasticene-common/src/main/java/com/plasticene/boot/common/pojo/PageResult.java)

```JAVA
public interface BaseMapperX<T> extends BaseMapper<T> {
    default PageResult<T> selectPage(PageParam pageParam, @Param("ew") Wrapper<T> queryWrapper) {
        // MyBatis Plus 查询
        IPage<T> mpPage = MybatisUtils.buildPage(pageParam);
        selectPage(mpPage, queryWrapper);
        // 转换返回
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal(), mpPage.getPages());
    }
}

```

mybatis-plus提供的mapper层`insertBatch()`方法，遍历数组，逐条插入数据库中，适合**少量**数据插入，或者对**性能要求不高**的场景。绝大多数场景下，推荐使用 MyBatis Plus 提供的 IService 的 [`#saveBatch()` ](https://github.com/baomidou/mybatis-plus/blob/34ebdf6ee6/mybatis-plus-extension/src/main/java/com/baomidou/mybatisplus/extension/service/IService.java#L66-L74)方法

### 5.条件构造器

继承 MyBatis Plus 的条件构造器，拓展了 [LambdaQueryWrapperX ](https://github.com/plasticene/plasticene-boot-starter-parent/blob/main/plasticene-boot-starter-mybatis/src/main/java/com/plasticene/boot/mybatis/core/query/LambdaQueryWrapperX.java)和 [QueryWrapperX](https://github.com/plasticene/plasticene-boot-starter-parent/blob/main/plasticene-boot-starter-mybatis/src/main/java/com/plasticene/boot/mybatis/core/query/QueryWrapperX.java)类，主要是增加 xxxIfPresent 方法，用于判断值不存在的时候，不要拼接到条件中，提供流式拼接条件。示例如下：

```java
    public PageResult<RoleDO> selectPage(RolePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<RoleDO>()
                .likeIfPresent(RoleDO::getName, reqVO.getName())
                .likeIfPresent(RoleDO::getCode, reqVO.getCode())
                .eqIfPresent(RoleDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(BaseDO::getCreateTime, reqVO.getBeginTime(), reqVO.getEndTime())
                .orderByDesc(RoleDO::getId));
    }
```

### 6.字段加密

[EncryptTypeHandler](https://github.com/plasticene/plasticene-boot-starter-parent/blob/main/plasticene-boot-starter-mybatis/src/main/java/com/plasticene/boot/mybatis/core/handlers/EncryptTypeHandler.java)：实现数据库的字段加密与解密。

默认提供了基于base64加密算法[Base64EncryptService](https://github.com/plasticene/plasticene-boot-starter-parent/blob/main/plasticene-boot-starter-mybatis/src/main/java/com/plasticene/boot/mybatis/core/encrypt/Base64EncryptService.java)和AES加密算法[AESEncryptService](https://github.com/plasticene/plasticene-boot-starter-parent/blob/main/plasticene-boot-starter-mybatis/src/main/java/com/plasticene/boot/mybatis/core/encrypt/AESEncryptService.java)，当然业务侧也可以自定义加密算法，这需要实现接口[EncryptService](https://github.com/plasticene/plasticene-boot-starter-parent/blob/main/plasticene-boot-starter-mybatis/src/main/java/com/plasticene/boot/mybatis/core/encrypt/EncryptService.java)，并把实现类注入到容器中即可。加密功能核心逻辑：

```java
@Bean
@ConditionalOnMissingBean(EncryptService.class)
public EncryptService encryptService() {
  Algorithm algorithm = encryptProperties.getAlgorithm();
  EncryptService encryptService;
  switch (algorithm) {
    case BASE64:
      encryptService =  new Base64EncryptService();
      break;
    case AES:
      encryptService = new AESEncryptService();
      break;
    default:
      encryptService =  null;
  }
  return encryptService;
}
```

### 7.逻辑删除与主键

所有表通过 `deleted` 字段来实现逻辑删除，值为 0 表示未删除，值为 1 表示已删除

```yml
#mybatis
mybatis-plus:
  mapper-locations: classpath*:/mapper/*.xml
  #实体扫描，多个package用逗号或者分号分隔
  typeAliasesPackage: com.shepherd.fast.entity
  configuration:
    map-underscore-to-camel-case: true # 虽然默认为 true ，但是还是显示去指定下。
    global-config:
      db-config:
        id-type: NONE # “智能”模式，基于 IdTypeEnvironmentPostProcessor + 数据源的类型，自动适配成 AUTO、INPUT 模式。
        #      id-type: AUTO # 自增 ID，适合 MySQL 等直接自增的数据库
        #      id-type: INPUT # 用户输入 ID，适合 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库
        #      id-type: ASSIGN_ID # 分配 ID，默认使用雪花算法。注意，Oracle、PostgreSQL、Kingbase、DB2、H2 数据库时，需要去除实体类上的 @KeySequence 注解
        logic-delete-value: 1 # 逻辑已删除值(默认为 1)
        logic-not-delete-value: 0 # 逻辑未删除值(默认为 0)
```

注意： mybatis-plus封装的SELECT 查询，都会自动拼接 `WHERE deleted = 0` 查询条件，过滤已经删除的记录。但是需要手写SQL实现查询，那么只能通过在 XML 或者 `@SELECT` 来写 SQL 语句是手动加上`WHERE deleted = 0`条件

`id` **默认**采用数据库自增的策略，如果希望使用 Snowflake 雪花算法，那么如上修改mybatis-plus配置