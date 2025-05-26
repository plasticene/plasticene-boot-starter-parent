package com.plasticene.boot.web.core.validator;

import com.plasticene.boot.web.core.constant.ValidatorConstant;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/5/23
 */
public class CombineNotNullValidator implements ConstraintValidator<CombineNotNull, Object> {

    // 解析SpEL有一定开销, 缓存表达式
    private static final ConcurrentHashMap<String, Expression> EXPRESSION_CACHE = new ConcurrentHashMap<>();
    // SpelExpressionParser是线程安全的
    private static final ExpressionParser parser = new SpelExpressionParser();
    // 条件表达式字符串
    private String condition;


    @Override
    public void initialize(CombineNotNull constraintAnnotation) {
        condition = constraintAnnotation.condition();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // 表达式为空
        if (StringUtils.isBlank(condition)) {
            return true;
        }
        // 获取根对象（被校验的对象）
        RequestAttributes requestAttributes = Objects.requireNonNull(RequestContextHolder.getRequestAttributes());
        Object requestBody = requestAttributes.getAttribute(ValidatorConstant.VALID_REQUEST_BODY, RequestAttributes.SCOPE_REQUEST);
        if (requestBody == null) {
            return true;
        }
        Expression expression = EXPRESSION_CACHE.computeIfAbsent(
                condition,
                parser::parseExpression
        );
        Boolean result = expression.getValue(requestBody, Boolean.class);
        if (Boolean.FALSE.equals(result)) {
            return true;
        }
        return value != null;
    }
}
