package com.plasticene.boot.flow.core.impl.condition;

import com.plasticene.boot.flow.core.impl.condition.AllMatchOperator;
import com.plasticene.boot.flow.core.impl.condition.AnyMatchOperator;
import com.plasticene.boot.flow.core.impl.condition.EqualsOperator;
import com.plasticene.boot.flow.core.impl.condition.GreaterThanOperator;
import com.plasticene.boot.flow.core.impl.condition.GreaterThanOrEqualsOperator;
import com.plasticene.boot.flow.core.impl.condition.LessThanOperator;
import com.plasticene.boot.flow.core.impl.condition.LessThanOrEqualsOperator;
import com.plasticene.boot.flow.core.impl.condition.NotContainsOperator;
import com.plasticene.boot.flow.core.impl.condition.NotEqualsOperator;
import com.plasticene.boot.flow.core.impl.condition.Operator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ZFJ
 * @date 2025/9/3
 */
public class OperatorFactory {

    private static final List<Operator> OPERATOR_LIST = new ArrayList<>();

    private static final Map<String, Operator> OPERATOR_MAP;

    static {
        OPERATOR_LIST.add(new EqualsOperator());
        OPERATOR_LIST.add(new NotEqualsOperator());
        OPERATOR_LIST.add(new LessThanOperator());
        OPERATOR_LIST.add(new LessThanOrEqualsOperator());
        OPERATOR_LIST.add(new GreaterThanOperator());
        OPERATOR_LIST.add(new GreaterThanOrEqualsOperator());
        OPERATOR_LIST.add(new AllMatchOperator());
        OPERATOR_LIST.add(new AnyMatchOperator());
        OPERATOR_LIST.add(new NotContainsOperator());



        OPERATOR_MAP = OPERATOR_LIST.stream()
                .collect(Collectors.toMap(Operator::op, Function.identity()));
    }

    public static Operator getOperator(String op) {
        return OPERATOR_MAP.get(op);
    }
}
