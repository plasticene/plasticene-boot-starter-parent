package com.plasticene.boot.flow.core.dto;

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
    private Integer source;

    /**
     * 属性
     */
    private String attr;

    /**
     * 运算符
     */
    private String operator;

    /**
     * 输入值
     */
    private String inputValue;

}
