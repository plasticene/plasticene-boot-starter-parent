package com.plasticene.boot.flow.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 条件规则
 * @author ZFJ
 * @date 2025/9/2
 */
@Data
public class ProcessConditionRule {

    /**
     * 属性字段来源  0：表单  1：系统字段
     */
    @Schema(description = "属性字段来源  0：表单  1：系统字段")
    private Integer source;

    /**
     * 字段
     */
    @Schema(description = "字段")
    private String field;

    /**
     * 字段类型
     */
    @Schema(description = "字段类型 0：字符串  1：数字  2：日期  3：bool  4：集合字段")
    private Integer type;

    /**
     * 运算符
     */
    @Schema(description = "运算符")
    private String operator;

    /**
     * 输入值
     */
    @Schema(description = "输入值")
    private String inputValue;


}
