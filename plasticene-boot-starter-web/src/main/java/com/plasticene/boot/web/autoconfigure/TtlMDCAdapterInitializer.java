package com.plasticene.boot.web.autoconfigure;

import org.slf4j.TtlMDCAdapter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 *
 * 初始化TtlMDCAdapter实例，并替换MDC中的adapter对象
 *
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/14 13:55
 */
public class TtlMDCAdapterInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        //加载TtlMDCAdapter实例
        TtlMDCAdapter.getInstance();
    }
}