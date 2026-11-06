package com.plasticene.boot.flow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.plasticene.boot.mybatis.core.metadata.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ZFJ
 * @date 2025/11/19
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("flow_category")
public class FlowCategory extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 公司id
     */
    private Long orgId;
    /**
     * 分类名称
     */
    private String name;
    /**
     * 前端内置图标标识，空值使用默认图标
     */
    private String icon;

    /**
     * 分类标识，同一公司内唯一、不区分大小写
     */
    private String code;
    /**
     * 分类描述
     */
    private String description;
    /**
     * 序号
     */
    private Integer seq;
    /**
     * 是否系统内置  0：否  1：是
     */
    private Integer isSys;
}
