package com.plasticene.boot.flow.core.operator;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.enums.FieldTypeEnum;

import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 运算符接口
 * @author ZFJ
 * @date 2025/9/3
 */
public interface Operator {

    DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
            getDateTime(inputValue);
        }
    }

    /**
     * 校验数字运算符和字段类型是否匹配
     * @param fieldType 字段类型
     */
    default void checkNumberOperator(Integer fieldType) {
        boolean equalityOperator = Objects.equals(op(), "=") || Objects.equals(op(), "!=");
        boolean scalarType = Objects.equals(fieldType, FieldTypeEnum.STRING.getCode()) ||
                Objects.equals(fieldType, FieldTypeEnum.NUMBER.getCode()) ||
                Objects.equals(fieldType, FieldTypeEnum.DATE.getCode());
        boolean orderedType = Objects.equals(fieldType, FieldTypeEnum.NUMBER.getCode()) ||
                Objects.equals(fieldType, FieldTypeEnum.DATE.getCode());
        if ((equalityOperator && scalarType) || (!equalityOperator && orderedType)) {
            return;
        }
        throw new BizException("运算符:【{0}】不支持字段类型: {1}", op(), fieldType);
    }

    /**
     * 校验集合运算符和字段类型是否匹配。
     *
     * @param fieldType 字段类型
     */
    default void checkCollectionOperator(Integer fieldType) {
        if (!Objects.equals(fieldType, FieldTypeEnum.COLLECTION.getCode())) {
            throw new BizException("运算符:【{0}】仅支持集合字段类型", op());
        }
    }

    /**
     * 将集合值统一转换为字符串集合，兼容 Collection、数组和 JSON 数组字符串。
     *
     * @param value 集合值
     * @return 标准化后的集合
     */
    default Set<String> getCollectionValues(Object value) {
        Collection<?> values;
        try {
            if (value instanceof Collection<?> collection) {
                values = collection;
            } else if (value != null && value.getClass().isArray()) {
                int length = Array.getLength(value);
                Set<Object> arrayValues = new HashSet<>(length);
                for (int index = 0; index < length; index++) {
                    arrayValues.add(Array.get(value, index));
                }
                values = arrayValues;
            } else if (value instanceof String text && JSONUtil.isTypeJSONArray(text)) {
                values = JSONUtil.parseArray(text);
            } else {
                throw new BizException("集合字段值必须是数组");
            }
            Set<String> result = new HashSet<>();
            values.forEach(item -> result.add(String.valueOf(item)));
            return result;
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException("集合字段值格式不正确");
        }
    }

    /**
     * 校验并解析条件中的集合值。
     *
     * @param fieldType 字段类型
     * @param inputValue 条件值
     * @return 标准化后的集合
     */
    default Set<String> validateCollectionInput(Integer fieldType, String inputValue) {
        checkCollectionOperator(fieldType);
        if (StrUtil.isBlank(inputValue)) {
            throw new BizException("条件输入值不能为空");
        }
        Set<String> values = getCollectionValues(inputValue);
        if (values.isEmpty()) {
            throw new BizException("集合类型条件至少选择一个值");
        }
        return values;
    }

    /**
     * 将日期时间字段值转换为 LocalDateTime。
     *
     * @param fieldValue 日期时间字段值
     * @return 日期时间
     */
    default LocalDateTime getDateTime(Object fieldValue) {
        if (!(fieldValue instanceof String value) || StrUtil.isBlank(value)) {
            throw new BizException("日期时间字段值必须是 yyyy-MM-dd HH:mm:ss 格式的字符串");
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BizException("日期时间字段值必须是 yyyy-MM-dd HH:mm:ss 格式的字符串");
        }
    }




}
