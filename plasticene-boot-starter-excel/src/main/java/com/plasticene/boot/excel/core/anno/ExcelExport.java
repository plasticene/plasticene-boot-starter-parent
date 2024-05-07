package com.plasticene.boot.excel.core.anno;

import java.lang.annotation.*;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2024/5/7 22:40
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelExport {

    String name() default "";

    String sheetName() default "sheet1";


}
