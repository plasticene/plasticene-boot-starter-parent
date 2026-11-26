package com.plasticene.boot.flow.business.fields.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 存储单个申请字段的展示值和类型化检索值。
 * @author ZFJ
 * @since 2026-11-20
 */
@Data
@TableName("flow_instance_field_value")
public class FlowInstanceFieldValue {

    /** 检索记录主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属组织及流程实例。 */
    private Long orgId;
    private Long instanceId;
    /** 规范化字段标识。 */
    private String fieldCode;
    /** 列表值类型。 */
    private String valueKind;
    /** 文本或选项原值及大小写无关的匹配值。 */
    private String textValue;
    private String matchValue;
    /** 数值和日期检索值。 */
    private BigDecimal numberValue;
    private LocalDateTime dateValue;
    /** 创建人与创建时间。 */
    private Long creator;
    private LocalDateTime createTime;
    /** 更新人与更新时间。 */
    private Long updater;
    private LocalDateTime updateTime;
}
