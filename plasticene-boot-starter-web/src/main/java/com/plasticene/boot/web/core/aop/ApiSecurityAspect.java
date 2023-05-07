package com.plasticene.boot.web.core.aop;

import com.alibaba.fastjson.JSONObject;
import com.plasticene.boot.common.constant.OrderConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.web.core.anno.ApiSecurity;
import com.plasticene.boot.web.core.global.RequestBodyWrapper;
import com.plasticene.boot.web.core.model.ApiSecurityParam;
import com.plasticene.boot.web.core.prop.ApiSecurityProperties;
import com.plasticene.boot.web.core.utils.AESUtil;
import com.plasticene.boot.web.core.utils.RSAUtil;
import com.plasticene.boot.web.core.utils.SignUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2023/5/4 10:19
 */
@Aspect
@Slf4j
@Order(value = OrderConstant.AOP_API_DECRYPT)
public class ApiSecurityAspect {
    @Resource
    private ApiSecurityProperties apiSecurityProperties;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String NONCE_KEY = "x-nonce-";

    @Pointcut("execution(* com.plasticene..controller..*(..)) && " +
            "(@annotation(com.plasticene.boot.web.core.anno.ApiSecurity) ||" +
            " @target(com.plasticene.boot.web.core.anno.ApiSecurity))")
    public void securityPointcut(){}

    @Around("securityPointcut()")
    public Object aroundApiSecurity(ProceedingJoinPoint joinPoint) throws Throwable {
        //=======AOP解密切面通知=======
        ApiSecurity apiSecurity = getApiSecurity(joinPoint);
        boolean isSign = apiSecurity.isSign();
        boolean decryptRequest = apiSecurity.decryptRequest();
        // 获取request加密传递的参数
        HttpServletRequest request = getRequest();
        // 只能针对post接口的请求参数requestBody进行统一加解密和加签，这是规定
        if (!Objects.equals("POST", request.getMethod())) {
            throw new BizException("只能POST接口才能加密加签操作");
        }
        // 获取controller接口方法定义的参数
        Object[] args = joinPoint.getArgs();
        Object[] newArgs = args;
        ApiSecurityParam apiSecurityParam = new ApiSecurityParam();
        // 请求参数解密
        if (decryptRequest) {
            // 不支持多个请求，因为解密请求参数之后会json字符串，再根据请求参数的类型映射过去，如果有多个参数就不知道映射关系了
            if (args.length > 1) {
                throw new BizException("加密接口方法只支持一个参数，请修改");
            }
            // args.length=0没有请求参数，就说明没必要解密，因为接口压根不接收参数，即使使用者无脑开启的该接口的参数加密，这里不做任何逻辑即可
            if (args.length == 1) {
                RequestBodyWrapper requestBodyWrapper;
                if (request instanceof RequestBodyWrapper) {
                    requestBodyWrapper = (RequestBodyWrapper) request;
                } else {
                    requestBodyWrapper = new RequestBodyWrapper(request);
                }
                String body = requestBodyWrapper.getBody();
                apiSecurityParam = JSONObject.parseObject(body, ApiSecurityParam.class);
                // 通过RSA私钥解密获取到aes秘钥
                String aesKey = RSAUtil.decryptByPrivateKey(apiSecurityParam.getKey(), apiSecurityProperties.getRsaPrivateKey());
                // 通过aes秘钥解密data参数数据
                String data = AESUtil.decrypt(apiSecurityParam.getData(), aesKey);
                //获取接口入参的类
                Class<?> c = args[0].getClass();
                //将获取解密后的真实参数，封装到接口入参的类中
                Object o = JSONObject.parseObject(data, c);
                newArgs = new Object[]{o};
            }
        }
        // 验签
        if (isSign) {
            verifySign(request, newArgs.length == 0 ? null : newArgs[0], apiSecurityParam);
        }
        return joinPoint.proceed(newArgs);
    }

    void verifySign(HttpServletRequest request, Object o, ApiSecurityParam apiSecurityParam) {
        // 如果请求参数是加密传输的，那就先从ApiSecurityParam获取签名和时间戳等等。
        // 如果请求参数不是加密传输的，那么ApiSecurityParam的字段取值都为null，这时候在请求的header里面获取参数信息
        String sign = apiSecurityParam.getSign();
        if (StringUtils.isBlank(sign)) {
            sign = request.getHeader("X-Sign");
        }
        if (StringUtils.isBlank(sign)) {
            throw new BizException("签名不能为空");
        }

        String nonce = apiSecurityParam.getNonce();
        if (StringUtils.isBlank(nonce)) {
            nonce = request.getHeader("X-Nonce");
        }
        if (StringUtils.isBlank(nonce)) {
            throw new BizException("唯一标识不能为空");
        }

        String timestamp = apiSecurityParam.getTimestamp();
        Long t;
        if (StringUtils.isBlank(timestamp)) {
            timestamp = request.getHeader("X-Timestamp");
        }
        if (StringUtils.isBlank(timestamp)) {
            throw new BizException("时间戳不能为空");
        } else {
            try {
                t = Long.valueOf(timestamp);
            } catch (Exception e) {
                throw new BizException("非法的时间戳");
            }
        }

        // 判断timestamp时间戳与当前时间是否超过签名有效时长（过期时间根据业务情况进行配置）,如果超过了就提示签名过期
        long now = System.currentTimeMillis() / 1000;
        if (now - t > apiSecurityProperties.getValidTime()) {
            throw new BizException("签名已过期");
        }

        // 判断nonce
        boolean nonceExists = stringRedisTemplate.hasKey(NONCE_KEY + nonce);
        if (nonceExists) {
            //请求重复
            throw new BizException("唯一标识nonce已存在");
        }

        // 验签
        SortedMap sortedMap = SignUtil.beanToMap(o);
        String content = SignUtil.getContent(sortedMap, nonce, timestamp);
        boolean flag = RSAUtil.verifySignByPublicKey(content, sign, apiSecurityProperties.getRsaPublicKey());
        if (!flag) {
            throw new BizException("签名验证不通过");
        }

        stringRedisTemplate.opsForValue().set(NONCE_KEY+ nonce, "1", apiSecurityProperties.getValidTime(),
                TimeUnit.SECONDS);
    }


    private HttpServletRequest getRequest() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        return request;
    }


    private ApiSecurity getApiSecurity(JoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        ApiSecurity apiSecurity = method.getAnnotation(ApiSecurity.class);
        if (Objects.isNull(apiSecurity)) {
            apiSecurity = method.getDeclaringClass().getAnnotation(ApiSecurity.class);
        }
        return apiSecurity;
    }
}
