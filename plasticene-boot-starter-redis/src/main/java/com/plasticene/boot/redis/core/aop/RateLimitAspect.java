package com.plasticene.boot.redis.core.aop;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.plasticene.boot.common.constant.OrderConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.redis.core.anno.RateLimit;
import com.plasticene.boot.redis.core.enums.LimitType;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/18 16:58
 */
@Aspect
@Order(OrderConstant.AOP_RATE_LIMIT)
public class RateLimitAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);

    private static final String UNKNOWN = "unknown";

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ApplicationContext applicationContext;


    /**
     * 切面逻辑
     */
    @Around("execution(public * *(..)) && @annotation(com.plasticene.boot.redis.core.anno.RateLimit)")
    public Object interceptor(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        LimitType limitType = rateLimit.limitType();
        String name = rateLimit.name();
        if (StrUtil.isBlank(name)) {
            // 获取spring.application.name服务名称
            name = applicationContext.getId();
        }
        if (StrUtil.isBlank(name)) {
            throw new BizException("分区name不能为空");
        }

        // 根据限流类型获取不同的key ,如果不传我们会以方法名作为key
        String key;
        switch (limitType) {
            case IP:
                key = getIpAddress();
                break;
            case CUSTOM:
                key = rateLimit.key();
                if (StrUtil.isBlank(key)) {
                    key = signature.getDeclaringTypeName() + "." + signature.getName();
                }
                break;
            default:
                key = signature.getDeclaringTypeName() + "." + signature.getName();
        }
        if (StrUtil.isNotBlank(rateLimit.prefix())) {
            key = rateLimit.prefix() + ":" + key;
        }
        key = name + ":" + key;
        List<String> keys = ListUtil.of(key);
        int limitPeriod = rateLimit.period();
        int limitCount = rateLimit.count();
        String luaScript = buildLuaScript();
        RedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);
        Long count = stringRedisTemplate.execute(redisScript, keys, String.valueOf(limitCount), String.valueOf(limitPeriod));
        logger.info("Access try count is {} for name={} and key = {}", count, name, key);
        if (count <= limitCount) {
            return pjp.proceed();
        } else {
            throw new BizException("Too Many Requests");
        }
    }

    /**
     * 编写 redis Lua 限流脚本
     */
    public String buildLuaScript() {
        return """
                local c
                c = redis.call('get',KEYS[1])
                if c and tonumber(c) > tonumber(ARGV[1]) then
                return c;
                end
                c = redis.call('incr',KEYS[1])
                if tonumber(c) == 1 then
                redis.call('expire',KEYS[1],ARGV[2])
                end
                return c;
                """;
    }


    public String getIpAddress() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        Objects.requireNonNull(requestAttributes, "requestAttributes is null");
        HttpServletRequest request = requestAttributes.getRequest();
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
