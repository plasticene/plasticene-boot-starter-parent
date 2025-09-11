package com.plasticene.boot.flow.core.operator;

import cn.hutool.core.util.NumberUtil;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.enums.FieldTypeEnum;

import java.util.Date;
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


    /**
     * 对条件输入值进行校验
     * @param fieldType 字段类型
     * @param inputValue 输入值
     */
    default void checkInputValue(Integer fieldType, String inputValue) {
        if (inputValue == null) {
            throw new BizException("条件输入值不能为空");
        }
        if (Objects.equals(fieldType, FieldTypeEnum.NUMBER.getCode())) {
            if (!NumberUtil.isNumber(inputValue)) {
                throw new BizException("数字类型字段请输入有效数字");
            }
        }
        if (Objects.equals(fieldType, FieldTypeEnum.DATE.getCode())) {
            // 日期时间类型统一输入时间戳，好比较
            if (!NumberUtil.isNumber(inputValue)) {
                throw new BizException("日期时间字段类型需要提供时间戳");
            }
        }
    }

    /**
     * 校验数字运算符和字段类型是否匹配
     * @param fieldType 字段类型
     */
    default void checkNumberOperator(Integer fieldType) {
        if (Objects.equals(fieldType, FieldTypeEnum.COLLECTION.getCode())) {
            throw new BizException("运算符:【{0}】不支持集合字段类型", op());
        }
        if (Objects.equals(fieldType, FieldTypeEnum.STRING.getCode()) &&
                !(Objects.equals(op(), "=") || Objects.equals(op(), "!="))) {
            throw new BizException("运算符:【{0}】不支持字符串类型", op());
        }
    }

    /**
     * 将日期时间字段值转换成时间戳
     * @param fieldValue 日期时间字段值
     * @return 时间戳
     */
    default long getTimestamp(Object fieldValue) {
        if (fieldValue instanceof Date date) {
            return date.getTime();
        }
        return (long) fieldValue;
    }




}
