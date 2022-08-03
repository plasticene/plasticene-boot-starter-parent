package com.plasticene.boot.license.core.anno;

import java.lang.annotation.*;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/8/3 13:50
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface License {

}
