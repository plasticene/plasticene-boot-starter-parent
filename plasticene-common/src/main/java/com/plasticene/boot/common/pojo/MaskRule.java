package com.plasticene.boot.common.pojo;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2024/4/30 10:04
 */
@Data
public class MaskRule {
    /**
     * 字段英文名称
     */
    @NotEmpty
    private String name;
    /**
     * 0：隐藏，1：显示
     */
    @NotNull
    private Integer type;
    /**
     * 规则：开头:0 中间:1 末尾: -1 全部: 2 区间：3
     */
    @NotNull
    private Integer scope;
    /**
     * 位数
     */
    private Integer count;
    /**
     * 开始位数
     */
    private Integer start;

    /**
     * 结束位数
     */
    private Integer end;
}
