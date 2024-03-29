package com.plasticene.boot.web.core.advice;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.web.core.anno.ApiSecurity;
import com.plasticene.boot.web.core.anno.ResponseResultBody;
import com.plasticene.boot.web.core.prop.ApiSecurityProperties;
import com.plasticene.boot.web.core.utils.AESUtil;
import com.plasticene.boot.web.core.utils.RSAUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/12 18:34
 */
@RestControllerAdvice
@Slf4j
public class ResponseResultBodyAdvice implements ResponseBodyAdvice<Object> {
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private ApiSecurityProperties apiSecurityProperties;


    /**
     * 判断类或者方法是否使用了 @ResponseResultBody
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), ResponseResultBody.class)
                || returnType.hasMethodAnnotation(ResponseResultBody.class)
                || AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), ApiSecurity.class)
                || returnType.hasMethodAnnotation(ApiSecurity.class);
    }

    /**
     * 当类或者方法使用了 @ResponseResultBody 就会调用这个方法
     * 如果返回类型是string，那么springmvc是直接返回的，此时需要手动转化为json
     * 因为当body都为null时，下面的非加密下的if判断参数类型的条件都不满足，如果接口返回类似为String，
     * 会报错com.shepherd.fast.global.ResponseVO cannot be cast to java.lang.String
     */
    @SneakyThrows
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        Method method = returnType.getMethod();
        Class<?> returnClass = method.getReturnType();
        Boolean enable = apiSecurityProperties.getEnable();
        ApiSecurity apiSecurity = method.getAnnotation(ApiSecurity.class);
        if (Objects.isNull(apiSecurity)) {
            apiSecurity = method.getDeclaringClass().getAnnotation(ApiSecurity.class);
        }
        if (enable && Objects.nonNull(apiSecurity) && apiSecurity.encryptResponse() && Objects.nonNull(body)) {
            // 只需要加密返回data数据内容
            if (body instanceof ResponseVO) {
                body = ((ResponseVO) body).getData();
            }
            JSONObject jsonObject = encryptResponse(body);
            body = jsonObject;
        } else {
            if (body instanceof String || Objects.equals(returnClass, String.class)) {
                String value = objectMapper.writeValueAsString(ResponseVO.success(body));
                return value;
            }
            // 防止重复包裹的问题出现
            if (body instanceof ResponseVO) {
                return body;
            }
        }
        return ResponseVO.success(body);
    }

    JSONObject encryptResponse(Object result) {
        String aseKey = AESUtil.generateAESKey();
        String content = JSONObject.toJSONString(result);
        String data = AESUtil.encrypt(content, aseKey);
        String key = RSAUtil.encryptByPublicKey(aseKey, apiSecurityProperties.getRsaPublicKey());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", key);
        jsonObject.put("data", data);
        return jsonObject;
    }

}


