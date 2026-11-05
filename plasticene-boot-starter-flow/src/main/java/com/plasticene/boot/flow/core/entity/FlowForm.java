package com.plasticene.boot.flow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.plasticene.boot.mybatis.core.metadata.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 保存独立流程表单及设计器配置。
 * @author ZFJ
 * @since 2026-11-05
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "flow_form", autoResultMap = true)
public class FlowForm extends BaseDO {
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

    /** 前端内置图标标识，空值使用默认图标 */
    private String icon;


    /**
     * 状态 0：关闭  1：开启
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /** 表单配置，包含 option 选项和 rule 规则。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> formConfig;
    /** 表单分类编码，关联流程分类。 */
    private String category;
}
