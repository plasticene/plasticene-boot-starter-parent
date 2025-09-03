package com.plasticene.boot.flow.core.operator;

/**
 * 运算符接口
 * @author ZFJ
 * @date 2025/9/3
 */
public interface Operator {

    /**
     * 运算符比较
     * @param fieldType 字段类型
     * @param filedValue 字段值
     * @param inputValue 输入的条件比较值
     * @return true or false
     */
    boolean compare(Integer fieldType, Object filedValue, String inputValue);

    /**
     * 校验运算符的合法性
     * @param filedType 字段类型
     * @param inputValue 输入的条件比较值
     */
    void validate(Integer filedType, String inputValue);

    /**
     * 运算符符号标识
     * @return 符号
     */
    String op();




}
