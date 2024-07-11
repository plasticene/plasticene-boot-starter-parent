package com.plasticene.boot.web.core.advice;

import cn.hutool.core.io.IoUtil;
import com.alibaba.fastjson.JSON;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.web.core.anno.ApiSecurity;
import com.plasticene.boot.web.core.global.PtcHttpInputMessage;
import com.plasticene.boot.web.core.model.ApiSecurityParam;
import com.plasticene.boot.web.core.prop.ApiSecurityProperties;
import com.plasticene.boot.web.core.utils.AESUtil;
import com.plasticene.boot.web.core.utils.RSAUtil;
import com.plasticene.boot.web.core.utils.SignUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2024/7/10 13:57
 */
@RestControllerAdvice
public class RequestBodyHandlerAdvice implements RequestBodyAdvice {
    @Resource
    private ApiSecurityProperties apiSecurityProperties;
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    private static final String SIGN_KEY = "X-Sign";
    private static final String NONCE_KEY = "X-Nonce";
    private static final String TIMESTAMP_KEY = "X-Timestamp";


    /**
     *
     * @param methodParameter 包含控制器方法的参数信息
     * @param targetType  目标类型，即请求体将要转换成的 Java 类型
     * @param converterType 将要使用的消息转换器的类型
     * @return
     */
    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return methodParameter.hasMethodAnnotation(ApiSecurity.class)
                || AnnotatedElementUtils.hasAnnotation(methodParameter.getDeclaringClass(), ApiSecurity.class);
    }

    /**
     * 接口入参解密
     * @param inputMessage 包含 HTTP 请求的头和体
     * @param parameter  包含控制器方法的参数信息
     * @param targetType  目标类型，即请求体将要转换成的 Java 类型
     * @param converterType  将要使用的消息转换器的类型
     * @return   返回新的流
     * @throws IOException
     */
    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        ApiSecurity apiSecurity = getApiSecurity(parameter);
        // 判断接口参数是否需要解密
        boolean decryptRequest = apiSecurity.decryptRequest();
        if (!decryptRequest) {
            return inputMessage;
        }
        InputStream inputStream = inputMessage.getBody();
        String body = IoUtil.read(inputStream, StandardCharsets.UTF_8);
        if (StringUtils.isBlank(body)) {
            throw new BizException("请求参数body不能为空");
        }
        // 加密传参格式固定为ApiSecurityParam
        ApiSecurityParam apiSecurityParam = JSON.parseObject(body, ApiSecurityParam.class);
        // 通过RSA私钥解密获取到aes秘钥
        String aesKey = RSAUtil.decryptByPrivateKey(apiSecurityParam.getKey(), apiSecurityProperties.getRsaPrivateKey());
        // 通过aes秘钥解密data参数数据，即真正实际的接口参数
        String data = AESUtil.decrypt(apiSecurityParam.getData(), aesKey);

        // 加密传参ApiSecurityParam可以接收签名参数，这里把签名参数放到header里面，方便在后面afterBodyRead中验签
        HttpHeaders headers = inputMessage.getHeaders();
        String timestamp = apiSecurityParam.getTimestamp();
        if (StringUtils.isNotBlank(timestamp)) {
            headers.set(TIMESTAMP_KEY, timestamp);
        }
        String nonce = apiSecurityParam.getNonce();
        if (StringUtils.isNotBlank(nonce)) {
            headers.set(NONCE_KEY, nonce);
        }
        String sign = apiSecurityParam.getSign();
        if (StringUtils.isNotBlank(sign)) {
            headers.set(SIGN_KEY, sign);
        }

        // 使用解密后的数据构造新的读取流, Spring MVC后续读取解析转换为接口
        return new PtcHttpInputMessage(headers, data);
    }

    /**
     * 验签
     * @param body  已转换的 Java 对象，表示请求体的数据
     * 其余参数和上面的{@link #beforeBodyRead(HttpInputMessage, MethodParameter, Type, Class)} 一样
     */
    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        ApiSecurity apiSecurity = getApiSecurity(parameter);
        boolean isSign = apiSecurity.isSign();
        if (!isSign) {
            return body;
        }
        // 验证签名sign
        verifySign(inputMessage.getHeaders(), body);
        return body;
    }

    /**
     * 和 {@link #afterBodyRead(Object, HttpInputMessage, MethodParameter, Type, Class)}一样
     * 只是这里处理body为空的这种情况，比如当body位空时，返回一个默认对象啥的
     */
    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                  Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return null;
    }

    ApiSecurity getApiSecurity(MethodParameter methodParameter) {
        ApiSecurity apiSecurity = methodParameter.getMethodAnnotation(ApiSecurity.class);
        return apiSecurity;
    }

    void verifySign(HttpHeaders headers, Object body) {
        // 如果请求参数是加密传输的，先从ApiSecurityParam获取签名和时间戳放到headers里面这里读取。
        // 如果请求参数是非加密即明文传输的，那签名参数只能放到header中
        String sign = headers.getFirst(SIGN_KEY);
        if (StringUtils.isBlank(sign)) {
            throw new BizException("签名不能为空");
        }

        String nonce = headers.getFirst(NONCE_KEY);
        if (StringUtils.isBlank(nonce)) {
            throw new BizException("唯一标识不能为空");
        }

        String timestamp = headers.getFirst(TIMESTAMP_KEY);
        if (StringUtils.isBlank(timestamp)) {
            throw new BizException("时间戳不能为空");
        }
        try {
            long time = Long.valueOf(timestamp);
            // 判断timestamp时间戳与当前时间是否超过签名有效时长（过期时间根据业务情况进行配置）,如果超过了就提示签名过期
            long now = System.currentTimeMillis() / 1000;
            if (now - time > apiSecurityProperties.getValidTime()) {
                throw new BizException("签名已过期");
            }
        } catch (Exception e) {
            throw new BizException("非法的时间戳");
        }

        // 判断nonce
        boolean nonceExists = stringRedisTemplate.hasKey(NONCE_KEY + nonce);
        if (nonceExists) {
            //请求重复
            throw new BizException("唯一标识nonce已存在");
        }

        // 验签
        SortedMap sortedMap = SignUtil.beanToMap(body);
        String content = SignUtil.getContent(sortedMap, nonce, timestamp);
        boolean flag = RSAUtil.verifySignByPublicKey(content, sign, apiSecurityProperties.getRsaPublicKey());
        if (!flag) {
            throw new BizException("签名验证不通过");
        }

        stringRedisTemplate.opsForValue().set(NONCE_KEY+ nonce, "1", apiSecurityProperties.getValidTime(),
                TimeUnit.SECONDS);
    }
}
