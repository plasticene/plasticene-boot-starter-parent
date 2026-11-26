package com.plasticene.boot.flow.business.fields.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.plasticene.boot.flow.core.api.vo.FlowBusinessFieldsVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 存储组织内单个流程分类的审批字段配置。
 * @author ZFJ
 * @since 2026-11-20
 */
@Data
@TableName(value = "flow_category_field", autoResultMap = true)
public class FlowCategoryField {

    /** 配置记录主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属组织。 */
    private Long orgId;
    /** 分类主键。 */
    private Long categoryId;
    /** 分类字段配置。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<FlowBusinessFieldsVO.Field> fields;
    /** 创建人与创建时间。 */
    private Long creator;
    private LocalDateTime createTime;
    /** 更新人与更新时间。 */
    private Long updater;
    private LocalDateTime updateTime;
}
