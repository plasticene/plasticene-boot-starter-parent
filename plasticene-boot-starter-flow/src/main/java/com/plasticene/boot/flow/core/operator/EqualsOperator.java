package com.plasticene.boot.flow.core.operator;

/**
 * @author ZFJ
 * @date 2025/9/3
 */
public class EqualsOperator implements Operator {
    @Override
    public boolean compare(Integer fieldType, Object filedValue, String inputValue) {
        return true;
    }

    @Override
    public void validate(Integer filedType, String inputValue) {

    }

    @Override
    public String op() {
        return "=";
    }
}
