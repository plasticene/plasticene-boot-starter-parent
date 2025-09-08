package com.plasticene.boot.flow.core.operator;

import cn.hutool.core.util.NumberUtil;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.enums.FieldTypeEnum;

import java.util.Objects;

/**
 * 运算符接口
 * @author ZFJ
 * @date 2025/9/3
 */
public interface Operator {

    /**
     * 运算符比较
     * @param fieldType 字段类型
     * @param fieldValue 字段值
     * @param inputValue 输入的条件比较值
     * @return true or false
     */
    boolean compare(Integer fieldType, Object fieldValue, String inputValue);

    /**
     * 校验运算符的合法性
     * @param fieldType 字段类型
     * @param inputValue 输入的条件比较值
     */
    void validate(Integer fieldType, String inputValue);

    /**
     * 运算符符号标识
     * @return 符号
     */
    String op();


    default void checkInputValue(Integer fieldType, String inputValue) {
        if (inputValue == null) {
            throw new BizException("条件输入值不能为空");
        }
        if (Objects.equals(fieldType, FieldTypeEnum.NUMBER.getCode())) {
            if (NumberUtil.isNumber(inputValue)) {
                return;
            }
        }
        if (Objects.equals(fieldType, FieldTypeEnum.DATE.getCode())) {
            // 日期时间类型统一输入时间戳，好比较
            if (NumberUtil.isNumber(inputValue)) {
                return;
            }
        }
        if (Objects.equals(fieldType, FieldTypeEnum.BOOL.getCode())) {
            if ("true".equalsIgnoreCase(inputValue) || "false".equalsIgnoreCase(inputValue)) {
                return;
            }
        }
        throw new BizException("输入的数据与类型不匹配");
    }




}
