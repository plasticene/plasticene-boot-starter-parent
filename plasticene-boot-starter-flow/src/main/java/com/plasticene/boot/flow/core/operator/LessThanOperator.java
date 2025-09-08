package com.plasticene.boot.flow.core.operator;

/**
 * @author ZFJ
 * @date 2025/9/8
 */
public class LessThanOperator implements Operator {
    @Override
    public boolean compare(Integer fieldType, Object fieldValue, String inputValue) {
        return false;
    }

    @Override
    public void validate(Integer fieldType, String inputValue) {

    }

    @Override
    public String op() {
        return "<";
    }
}
