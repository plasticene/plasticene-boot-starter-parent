package com.plasticene.boot.web.core.aop;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.plasticene.boot.common.constant.OrderConstant;
import com.plasticene.boot.web.core.anno.ApiLog;
import com.plasticene.boot.web.core.model.RequestInfo;
import com.plasticene.boot.web.core.prop.ApiLogProperties;
import com.plasticene.boot.web.core.utils.IpUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;


import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/15 17:30
 */
@Aspect
@Slf4j(topic = "ptc.api.log")
@Order(value = OrderConstant.AOP_API_LOG)
public class ApiLogPrintAspect {

    @Resource
    private ApiLogProperties apiLogProperties;


    /**
     * 声明切点
     *
     * @param joinPoint 切入点
     *表达式示例：
     * 任意公共方法的执行：execution(public * *(..))
     * 任何一个以“set”开始的方法的执行：execution(* set*(..))
     * AccountService 接口的任意方法的执行：execution(* com.xyz.service.AccountService.*(..))
     * 定义在service包里的任意方法的执行： execution(* com.xyz.service.*.*(..))
     * 定义在service包和所有子包里的任意类的任意方法的执行：execution(* com.xyz.service..*.*(..))
     * @return 返回值
     * @throws Throwable 异常
     */
    @Around("execution(* com.plasticene..controller..*(..))")
    public Object timeAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 这里做开关判断，而不是根据开关条件注入切面bean，是因为为了方便修改配置开关动态更新来控制开关打印接口参数日志
        if (!apiLogProperties.getEnable()) {
            return joinPoint.proceed();
        }
        ApiLog apiLog = getApiLog(joinPoint);
        if (Objects.isNull(apiLog)) {
            return joinPoint.proceed();
        }
        long start = System.currentTimeMillis();
        HttpServletRequest request = getRequest();
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setIp(IpUtil.getIpAddress(request));
        requestInfo.setUrl(request.getRequestURI());
        requestInfo.setHttpMethod(request.getMethod());
        requestInfo.setClassMethod(String.format("%s.%s", joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName()));
        requestInfo.setRequestParams(formatLogParam(getRequestParams(joinPoint, request)));
        log.info("Request Info : {}", JSON.toJSONString(requestInfo));

        Object result = joinPoint.proceed();

        log.info("Response Result:  {}", JSON.toJSONString(result));
        log.info("Time Cost: [{}]ms", System.currentTimeMillis() - start);
        return result;
    }

    private Object getRequestParams(ProceedingJoinPoint joinPoint, HttpServletRequest request) throws UnsupportedEncodingException {
        Object[] args = joinPoint.getArgs();
        Object params = null;
        String queryString = request.getQueryString();
        String method = request.getMethod();
        if (args.length > 0) {
             // 有body的接口类型，这时候要排除HttpServletRequest request, HttpServletResponse response作为接口方法参数
            if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)) {
                int length = args.length;
                int index = 0;
                Object object = null;
                while (index < length) {
                    Object o = args[index];
                    index++;
                    if (o instanceof HttpServletRequest || o instanceof HttpServletResponse) {
                        continue;
                    } else {
                        object = o;
                        break;
                    }
                }
                params = object;
                // 方法为get时，当接口参数为路径参数，那么此时queryString为null
            } else if ("GET".equals(method) && StrUtil.isNotBlank(queryString)) {
                params = URLDecoder.decode(queryString, StandardCharsets.UTF_8);
            }
        }
        return params;
    }


    private HttpServletRequest getRequest() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        assert requestAttributes != null;
        return requestAttributes.getRequest();
    }

    private ApiLog getApiLog(JoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        ApiLog apiLog = method.getAnnotation(ApiLog.class);
        if (Objects.isNull(apiLog)) {
            apiLog = method.getDeclaringClass().getAnnotation(ApiLog.class);
        }
        return apiLog;
    }

    private Object formatLogParam(Object object) {
        return toLogValue(object, new IdentityHashMap<>());
    }

    private static Object toLogValue(Object object, IdentityHashMap<Object, Boolean> visiting) {
        if (object == null || isSimpleValue(object.getClass())) {
            return object;
        }
        if (object instanceof MultipartFile) {
            return formatMultipartFile((MultipartFile) object);
        }
        if (visiting.containsKey(object)) {
            return String.valueOf(object);
        }
        visiting.put(object, Boolean.TRUE);
        try {
            if (object.getClass().isArray()) {
                return formatArray(object, visiting);
            }
            if (object instanceof Iterable) {
                return formatIterable((Iterable<?>) object, visiting);
            }
            if (object instanceof Map) {
                return formatMap((Map<?, ?>) object, visiting);
            }
            if (object.getClass().getName().startsWith("java.")) {
                return String.valueOf(object);
            }
            return formatBean(object, visiting);
        } finally {
            visiting.remove(object);
        }
    }

    private static Map<String, Object> formatMultipartFile(MultipartFile multipartFile) {
        Map<String, Object> fileInfo = new LinkedHashMap<>();
        fileInfo.put("name", multipartFile.getName());
        fileInfo.put("originalFilename", multipartFile.getOriginalFilename());
        fileInfo.put("size", multipartFile.getSize());
        fileInfo.put("contentType", multipartFile.getContentType());
        return fileInfo;
    }

    private static List<Object> formatArray(Object array, IdentityHashMap<Object, Boolean> visiting) {
        int length = Array.getLength(array);
        List<Object> values = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            values.add(toLogValue(Array.get(array, i), visiting));
        }
        return values;
    }

    private static List<Object> formatIterable(Iterable<?> iterable, IdentityHashMap<Object, Boolean> visiting) {
        List<Object> values = new ArrayList<>();
        for (Object value : iterable) {
            values.add(toLogValue(value, visiting));
        }
        return values;
    }

    private static Map<String, Object> formatMap(Map<?, ?> map, IdentityHashMap<Object, Boolean> visiting) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            values.put(String.valueOf(entry.getKey()), toLogValue(entry.getValue(), visiting));
        }
        return values;
    }

    private static Map<String, Object> formatBean(Object bean, IdentityHashMap<Object, Boolean> visiting) {
        Map<String, Object> values = new LinkedHashMap<>();
        Class<?> clazz = bean.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    values.put(field.getName(), toLogValue(field.get(bean), visiting));
                } catch (Exception ignored) {
                    values.put(field.getName(), "[unreadable]");
                }
            }
            clazz = clazz.getSuperclass();
        }
        return values;
    }

    private static boolean isSimpleValue(Class<?> clazz) {
        return clazz.isPrimitive()
                || CharSequence.class.isAssignableFrom(clazz)
                || Number.class.isAssignableFrom(clazz)
                || Boolean.class == clazz
                || Character.class == clazz
                || Date.class.isAssignableFrom(clazz)
                || BigDecimal.class.isAssignableFrom(clazz)
                || BigInteger.class.isAssignableFrom(clazz)
                || clazz.isEnum();
    }


}
