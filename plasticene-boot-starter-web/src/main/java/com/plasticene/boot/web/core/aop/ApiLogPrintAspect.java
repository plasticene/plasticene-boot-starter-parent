package com.plasticene.boot.web.core.aop;

import cn.hutool.core.util.StrUtil;
import com.plasticene.boot.common.constant.OrderConstant;
import com.plasticene.boot.common.utils.JsonUtils;
import com.plasticene.boot.web.core.anno.ApiLog;
import com.plasticene.boot.web.core.anno.ApiSecurity;
import com.plasticene.boot.web.core.model.RequestInfo;
import com.plasticene.boot.web.core.prop.ApiLogProperties;
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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.Objects;

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
        requestInfo.setIp(request.getRemoteAddr());
        requestInfo.setUrl(request.getRequestURL().toString());
        requestInfo.setHttpMethod(request.getMethod());
        requestInfo.setClassMethod(String.format("%s.%s", joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName()));
        requestInfo.setRequestParams(getRequestParams(joinPoint, request));
        log.info("Request Info : {}", JsonUtils.toJsonString(requestInfo));

        Object result = joinPoint.proceed();

        log.info("Response result:  {}", JsonUtils.toJsonString(result));
        log.info("time cost:  {}", System.currentTimeMillis() - start);
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
                if (object instanceof MultipartFile) {
                    MultipartFile multipartFile = (MultipartFile) object;
                    params = MessageFormat.format("文件名: {0}, 大小: {1}", multipartFile.getOriginalFilename(), multipartFile.getSize());
                } else {
                    params = object;
                }
                // 方法为get时，当接口参数为路径参数，那么此时queryString为null
            } else if ("GET".equals(method) && StrUtil.isNotBlank(queryString)) {
                params = URLDecoder.decode(queryString, "utf-8");
            }
        }
        return params;
    }


    private HttpServletRequest getRequest() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        return request;
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

}
