package com.plasticene.boot.web.core.aop;

import com.plasticene.boot.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/15 17:30
 */
@Aspect
@Slf4j
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
        // 定义返回对象、得到方法需要的参数
        Object obj = null;
        Object[] args = joinPoint.getArgs();
        long startTime = System.currentTimeMillis();
        this.printRequestInfo(joinPoint);

        try {
            obj = joinPoint.proceed(args);
            return obj;
        } finally {
            // 获取执行的方法名
            long endTime = System.currentTimeMillis();
            // 打印耗时的信息
            this.printResponseInfo(startTime, endTime, obj);
        }
    }

    private void printRequestInfo(ProceedingJoinPoint joinPoint) {
        try {
            HttpServletRequest request = getRequest();
            Object[] args = joinPoint.getArgs();
            String params = "";
            String queryString = request.getQueryString();
            String method = request.getMethod();
            if (args.length > 0) {
                if ("POST".equals(method) || "PUT".equals(method)) {
                    Object object = args[0];
                    if (object instanceof MultipartFile) {
                        MultipartFile multipartFile = (MultipartFile) object;
                        params = MessageFormat.format("文件:{0},大小:{1}", multipartFile.getOriginalFilename(), multipartFile.getSize());
                    } else {
                        params = JsonUtils.toJsonString(object);
                    }
                } else if ("GET".equals(method)) {
                    params = queryString;
                }
            }

            log.info("请求URI: [{}]，开始处理=========>", request.getRequestURI());
            log.info("HTTP METHOD:[{}], IP:[{}],CLASS_METHOD:[{}]",
                    method, this.getIpAddr(request), joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName());
            log.info("请求参数: {}", params);
        } catch (Exception ex) {
            log.error("日志打印出错", ex);
        }
    }

    private void printResponseInfo(long startTime, long endTime, Object obj) {
        try {
            long diffTime = endTime - startTime;
            log.info("返回结果:{} ", JsonUtils.toJsonString(obj));
            log.info("<=========处理完成，共消费时间:[{}]毫秒", diffTime);
        } catch (Exception ex) {
            log.error("日志打印出错", ex);
        }
    }

    public static HttpServletRequest getRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();
        return request;
    }

    private String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
