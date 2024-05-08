package com.plasticene.boot.excel.autoconfigure;

import com.plasticene.boot.excel.core.aop.ExcelExportAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2024/5/8 17:39
 */
@Configuration
public class PlasticeneExcelAutoConfiguration {


    @Bean
    public ExcelExportAspect excelExportAspect() {
        return new ExcelExportAspect();
    }
}
