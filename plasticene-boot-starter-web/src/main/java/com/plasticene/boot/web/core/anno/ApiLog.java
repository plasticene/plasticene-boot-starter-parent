package com.plasticene.boot.web.core.anno;

import java.lang.annotation.*;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2023/10/31 18:45
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface ApiLog {
}
