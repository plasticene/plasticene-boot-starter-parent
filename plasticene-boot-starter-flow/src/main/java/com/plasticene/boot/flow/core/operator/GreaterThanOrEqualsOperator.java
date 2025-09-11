package com.plasticene.boot.flow.core.operator;

/**
 * @author ZFJ
 * @date 2025/9/8
 */
public class GreaterThanOrEqualsOperator extends LessThanOperator implements Operator {
    @Override
    public boolean compare(Integer fieldType, Object fieldValue, String inputValue) {
        try {
            boolean compare = super.compare(fieldType, fieldValue, inputValue);
            return !compare;
        } catch (Exception e) {
            // 这里捕获父类抛出的异常返回false，父类已经打印异常了，这里不再重复打印
            return false;
        }

    }

    @Override
    public void validate(Integer fieldType, String inputValue) {
        super.validate(fieldType, inputValue);
    }

    @Override
    public String op() {
        return ">=";
    }
}
