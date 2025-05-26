package com.plasticene.boot.web.core.advice;

import com.plasticene.boot.web.core.constant.ValidatorConstant;
import jakarta.validation.Valid;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/5/23
 */
@RestControllerAdvice
public class ValidRequestBodyAdvice implements RequestBodyAdvice {
    @Override
    public boolean supports(@NonNull MethodParameter methodParameter,
                            @NonNull Type targetType,
                            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return methodParameter.hasMethodAnnotation(Validated.class)
                || methodParameter.hasParameterAnnotation(Validated.class)
                || AnnotatedElementUtils.hasAnnotation(methodParameter.getDeclaringClass(), Validated.class)
                || methodParameter.hasMethodAnnotation(Valid.class)
                || methodParameter.hasParameterAnnotation(Valid.class)
                || AnnotatedElementUtils.hasAnnotation(methodParameter.getDeclaringClass(), Valid.class);

    }

    @Override
    @NonNull
    public HttpInputMessage beforeBodyRead(
            @NonNull HttpInputMessage inputMessage,
            @NonNull MethodParameter parameter,
            @NonNull Type targetType,
            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return inputMessage;
    }

    @Override
    @NonNull
    public Object afterBodyRead(@NonNull Object body,
                                @NonNull HttpInputMessage inputMessage,
                                @NonNull MethodParameter parameter,
                                @NonNull Type targetType,
                                @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        RequestAttributes requestAttributes = Objects.requireNonNull(RequestContextHolder.getRequestAttributes());
        requestAttributes.setAttribute(ValidatorConstant.VALID_REQUEST_BODY, body, RequestAttributes.SCOPE_REQUEST);
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body,
                                  @NonNull HttpInputMessage inputMessage,
                                  @NonNull MethodParameter parameter,
                                  @NonNull Type targetType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return null;
    }
}
