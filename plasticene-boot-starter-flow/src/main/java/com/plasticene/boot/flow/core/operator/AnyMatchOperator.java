package com.plasticene.boot.flow.core.operator;

import java.util.Set;

/**
 * 集合任意匹配运算符。
 *
 * @author ZFJ
 * @since 2026-09-21
 */
public class AnyMatchOperator implements Operator {

    @Override
    public boolean compare(Integer fieldType, Object fieldValue, String inputValue) {
        if (fieldValue == null || inputValue == null) {
            return false;
        }
        Set<String> actual = getCollectionValues(fieldValue);
        Set<String> expected = validateCollectionInput(fieldType, inputValue);
        return actual.stream().anyMatch(expected::contains);
    }

    @Override
    public void validate(Integer fieldType, String inputValue) {
        validateCollectionInput(fieldType, inputValue);
    }

    @Override
    public String op() {
        return "anyMatch";
    }
}
