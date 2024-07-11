package com.plasticene.boot.web.core.anno;

import cn.hutool.core.annotation.Alias;

import java.lang.annotation.*;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2023/5/2 11:50
 *
 * 该注解用于标识 需要经过加密或者加签来加固接口安全性的接口
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface ApiSecurity {

    @Alias("isSign")
    boolean value() default true;

    /**
     * 是否加签验证，默认开启
     * @return
     */
    @Alias("value")
    boolean isSign() default true;

    /**
     * 接口请求参数是否需要解密
     * @return
     */
    boolean decryptRequest() default false;

    /**
     * 接口响应参数是否需要加密
     * @return
     */
    boolean encryptResponse() default false;
}
