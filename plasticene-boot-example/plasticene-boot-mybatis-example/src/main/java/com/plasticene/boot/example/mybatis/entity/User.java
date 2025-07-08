package com.plasticene.boot.example.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.plasticene.boot.mybatis.core.handlers.EncryptTypeHandler;
import com.plasticene.boot.mybatis.core.handlers.type.LongSetTypeHandler;
import com.plasticene.boot.mybatis.core.handlers.type.StringListTypeHandler;
import com.plasticene.boot.mybatis.core.metadata.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.*;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/6/24
 */


@EqualsAndHashCode(callSuper = true)
@Data
// 注意使用类型处理器时，必须设置autoResultMap = true，否则不生效
@TableName(value = "tb_user", autoResultMap = true)
public class User extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userNo;
    private Integer gender;
    private Date birthday;
    private Integer isDelete;
    private String address;

    // --------- 多租户
    private Long orgId;

    // --------- 加密存储字段
//    @TableField(typeHandler = EncryptTypeHandler.class)
    private String name;
//    @TableField(typeHandler = EncryptTypeHandler.class)
    private String phone;
//    @TableField(typeHandler = EncryptTypeHandler.class)
    private String email;

    // -------- 复合字段类型处理
    @TableField(typeHandler = LongSetTypeHandler.class)
    private Set<Long> roleId;
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> hobby;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> remark;
}
