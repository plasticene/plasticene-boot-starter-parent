package com.plasticene.boot.excel.core.anno;

import com.alibaba.excel.support.ExcelTypeEnum;

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

    String sheet() default "sheet1";

    ExcelTypeEnum suffix() default ExcelTypeEnum.XLSX;


}
