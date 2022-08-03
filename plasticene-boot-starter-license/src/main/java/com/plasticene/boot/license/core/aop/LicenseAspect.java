package com.plasticene.boot.license.core.aop;

import com.plasticene.boot.common.aspect.AbstractAspectSupport;
import com.plasticene.boot.common.constant.OrderConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.license.core.LicenseVerify;
import com.plasticene.boot.license.core.anno.License;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;

import javax.annotation.Resource;


/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/8/3 13:54
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
        boolean b = licenseVerify.verify();
        if (b) {
            return pjp.proceed();
        }
        return null;
    }

}
