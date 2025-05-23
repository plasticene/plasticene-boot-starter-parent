package com.plasticene.boot.web.core.advice;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.web.constant.ApiSecurityConstant;
import com.plasticene.boot.web.core.anno.ApiSecurity;
import com.plasticene.boot.web.core.anno.ResponseResultBody;
import com.plasticene.boot.web.core.model.ApiSecurityKey;
import com.plasticene.boot.web.core.prop.ApiSecurityProperties;
import com.plasticene.boot.web.core.utils.AESUtil;
import com.plasticene.boot.web.core.utils.RSAUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

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
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ApiSecurityProperties apiSecurityProperties;


    /**
     * 判断类或者方法是否使用了 @ResponseResultBody统一结果结构或者 @ApiSecurity接口参数加解密
     */
    @Override
    public boolean supports(MethodParameter returnType,
                            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), ResponseResultBody.class)
                || returnType.hasMethodAnnotation(ResponseResultBody.class)
                || AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), ApiSecurity.class)
                || returnType.hasMethodAnnotation(ApiSecurity.class);
    }

    /**
     * 如果接口返回类型为String,统一结构会报错ResponseVO cannot be cast to java.lang.String
     * 因为返回类型是string，那么springmvc是直接返回的，此时需要手动转化为json
     */
    @SneakyThrows
    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  @NonNull MediaType selectedContentType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @NonNull ServerHttpRequest request,
                                  @NonNull ServerHttpResponse response) {
        Method method = returnType.getMethod();
        Objects.requireNonNull(method, "method is null");
        Class<?> returnClass = method.getReturnType();
        Boolean enable = apiSecurityProperties.getEnable();
        ApiSecurity apiSecurity = method.getAnnotation(ApiSecurity.class);
        if (Objects.isNull(apiSecurity)) {
            apiSecurity = method.getDeclaringClass().getAnnotation(ApiSecurity.class);
        }
        if (enable && Objects.nonNull(apiSecurity) && apiSecurity.encryptResponse() && Objects.nonNull(body)) {
            // 只需要加密返回data数据内容
            if (body instanceof ResponseVO<?> responseVO) {
                body = responseVO.getData();
            }
            body = encryptResponse(body);
        } else {
            // 接口返回string类型，单独处理
            if (body instanceof String || Objects.equals(returnClass, String.class)) {
                return objectMapper.writeValueAsString(ResponseVO.success(body));
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
        String thirdRsaPublicKey = getThirdRsaPublicKey();
        String key = RSAUtil.encryptByPublicKey(aseKey, thirdRsaPublicKey);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", key);
        jsonObject.put("data", data);
        return jsonObject;
    }


    String getThirdRsaPublicKey() {
        RequestAttributes requestAttributes = Objects.requireNonNull(RequestContextHolder.getRequestAttributes());
        ApiSecurityKey apiSecurityKey = (ApiSecurityKey) requestAttributes.getAttribute(ApiSecurityConstant.API_SECURITY,
                RequestAttributes.SCOPE_REQUEST);
        return Objects.requireNonNull(apiSecurityKey).getThirdRsaPublicKey();
    }

}


