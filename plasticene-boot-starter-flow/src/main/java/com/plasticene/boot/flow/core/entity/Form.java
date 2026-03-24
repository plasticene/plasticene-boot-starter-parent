package com.plasticene.boot.flow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.plasticene.boot.mybatis.core.metadata.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author ZFJ
 * @since 2026/2/24
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "form", autoResultMap = true)
public class Form extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 公司id
     */
    private Long orgId;
    /**
     * 表单名称
     */
    private String name;

    /**
     * 状态 0：关闭  1：开启
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 表单配置
     */
    private String conf;

    /**
     * 表单字段
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> fields;
}
