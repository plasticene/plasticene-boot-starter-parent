package com.plasticene.boot.flow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.plasticene.boot.mybatis.core.metadata.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ZFJ
 * @date 2025/11/19
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Category extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 公司id
     */
    private Long orgId;
    /**
     * 分组名称
     */
    private String name;
    /**
     * 分组code
     */
    private String code;
    /**
     * 序号
     */
    private Integer seq;
    /**
     * 是否系统内置  0：否  1：是
     */
    private Integer isSys;
}
