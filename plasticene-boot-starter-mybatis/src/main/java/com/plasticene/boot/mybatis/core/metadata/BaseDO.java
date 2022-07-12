package com.plasticene.boot.mybatis.core.metadata;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/12 10:35
 */

/**
 * 数据库表字段公共属性抽象类
 */
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