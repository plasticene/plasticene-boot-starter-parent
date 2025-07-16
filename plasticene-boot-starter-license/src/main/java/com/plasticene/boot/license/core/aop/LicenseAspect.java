package com.plasticene.boot.license.core.aop;

import com.plasticene.boot.common.aspect.AbstractAspectSupport;
import com.plasticene.boot.common.constant.OrderConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.license.core.LicenseVerify;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;



/**
 * 通过该切面实现License到期后服务不可用验证机制
 * 验证license有性能开销不能频繁，所以这里切面判断逻辑必须简单直接
 * @author fjzheng
 * @date 2022/8/3
 */


@Aspect
@Order(OrderConstant.AOP_LICENSE)
public class LicenseAspect extends AbstractAspectSupport {

    @Resource
    private LicenseVerify licenseVerify;

    // 指定切入点为License注解
    @Pointcut("@annotation(com.plasticene.boot.license.core.anno.License)")
    public void licenseAnnotationPointcut() {
    }

    // 环绕通知
    @Around("licenseAnnotationPointcut()")
    public Object aroundLicense(ProceedingJoinPoint pjp) throws Throwable {
        boolean valid = licenseVerify.isValid();
        if (!valid) {
            throw new BizException("license is invalid");
        }
        return pjp.proceed();
    }

}
