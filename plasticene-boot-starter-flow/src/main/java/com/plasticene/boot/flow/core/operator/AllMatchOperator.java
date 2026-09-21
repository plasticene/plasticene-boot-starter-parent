package com.plasticene.boot.flow.core.operator;

import java.util.Set;

/**
 * 集合完全匹配运算符，忽略元素顺序。
 *
 * @author ZFJ
 * @since 2026-09-21
 */
public class AllMatchOperator implements Operator {

    @Override
    public boolean compare(Integer fieldType, Object fieldValue, String inputValue) {
        if (fieldValue == null || inputValue == null) {
            return false;
        }
        Set<String> expected = validateCollectionInput(fieldType, inputValue);
        return getCollectionValues(fieldValue).equals(expected);
    }

    @Override
    public void validate(Integer fieldType, String inputValue) {
        validateCollectionInput(fieldType, inputValue);
    }

    @Override
    public String op() {
        return "allMatch";
    }
}
