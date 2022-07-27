package com.plasticene.boot.web.core.aop;

import com.plasticene.boot.common.constant.OrderConstant;
import com.plasticene.boot.common.utils.JsonUtils;
import com.plasticene.boot.web.core.model.RequestInfo;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.MessageFormat;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/15 17:30
 */
@Aspect
@Slf4j
@Order(value = OrderConstant.AOP_API_LOG)
public class ApiLogPrintAspect {
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
            if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)) {
                Object object = args[0];
                if (object instanceof MultipartFile) {
                    MultipartFile multipartFile = (MultipartFile) object;
                    params = MessageFormat.format("文件名: {0}, 大小: {1}", multipartFile.getOriginalFilename(), multipartFile.getSize());
                } else {
                    params = object;
                }
            } else if ("GET".equals(method)) {
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

}
