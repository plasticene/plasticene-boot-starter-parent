package com.plasticene.boot.flow.core.operator;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.enums.FieldTypeEnum;
import com.plasticene.boot.flow.core.factory.OperatorFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 条件运算符测试。
 *
 * @author ZFJ
 * @since 2026-09-21
 */
class ConditionOperatorTest {

    @Test
    void shouldMatchCollectionIgnoringOrder() {
        Operator operator = OperatorFactory.getOperator("allMatch");

        assertTrue(operator.compare(FieldTypeEnum.COLLECTION.getCode(), List.of("a", 2), "[2,\"a\"]"));
        assertFalse(operator.compare(FieldTypeEnum.COLLECTION.getCode(), List.of("a", 2), "[2]"));
    }

    @Test
    void shouldMatchAnyCollectionValue() {
        Operator operator = OperatorFactory.getOperator("anyMatch");

        assertTrue(operator.compare(FieldTypeEnum.COLLECTION.getCode(), List.of("a", "b"), "[\"b\",\"c\"]"));
        assertFalse(operator.compare(FieldTypeEnum.COLLECTION.getCode(), List.of("a"), "[\"b\",\"c\"]"));
    }

    @Test
    void shouldMatchWhenCollectionContainsNoneOfExpectedValues() {
        Operator operator = OperatorFactory.getOperator("notContains");

        assertTrue(operator.compare(FieldTypeEnum.COLLECTION.getCode(), new String[]{"a"}, "[\"b\"]"));
        assertFalse(operator.compare(FieldTypeEnum.COLLECTION.getCode(), new String[]{"a"}, "[\"a\",\"b\"]"));
    }

    @Test
    void shouldRejectInvalidCollectionConditions() {
        Operator operator = OperatorFactory.getOperator("anyMatch");

        assertThrows(BizException.class, () -> operator.validate(FieldTypeEnum.STRING.getCode(), "[\"a\"]"));
        assertThrows(BizException.class, () -> operator.validate(FieldTypeEnum.COLLECTION.getCode(), "not-json"));
        assertThrows(BizException.class, () -> operator.validate(FieldTypeEnum.COLLECTION.getCode(), "[]"));
    }

    @Test
    void shouldRegisterCollectionOperators() {
        assertInstanceOf(AllMatchOperator.class, OperatorFactory.getOperator("allMatch"));
        assertInstanceOf(AnyMatchOperator.class, OperatorFactory.getOperator("anyMatch"));
        assertInstanceOf(NotContainsOperator.class, OperatorFactory.getOperator("notContains"));
    }

    @Test
    void shouldAllowOrderedComparisonOnlyForNumberAndDate() {
        Operator operator = OperatorFactory.getOperator(">");

        operator.validate(FieldTypeEnum.NUMBER.getCode(), "10");
        operator.validate(FieldTypeEnum.DATE.getCode(), "2026-09-21 14:30:00");
        assertThrows(BizException.class, () -> operator.validate(FieldTypeEnum.STRING.getCode(), "10"));
        assertThrows(BizException.class, () -> operator.validate(FieldTypeEnum.COLLECTION.getCode(), "10"));
        assertThrows(BizException.class, () -> operator.validate(FieldTypeEnum.DATE.getCode(), "1726848000000"));
    }

    @Test
    void shouldCompareDateTimeStrings() {
        Operator operator = OperatorFactory.getOperator(">");

        assertTrue(operator.compare(
                FieldTypeEnum.DATE.getCode(),
                "2026-09-22 00:00:00",
                "2026-09-21 23:00:00"
        ));
        assertTrue(OperatorFactory.getOperator("=").compare(
                FieldTypeEnum.DATE.getCode(),
                "2026-09-21 14:30:00",
                "2026-09-21 14:30:00"
        ));
    }
}
