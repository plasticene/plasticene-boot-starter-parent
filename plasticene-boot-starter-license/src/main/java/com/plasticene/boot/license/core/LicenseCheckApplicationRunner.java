package com.plasticene.boot.license.core;

import com.plasticene.boot.common.constant.OrderConstant;
import de.schlichtherle.license.LicenseContent;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/8/3 11:51
 */
@Order(OrderConstant.RUNNER_LICENSE)
public class LicenseCheckApplicationRunner implements ApplicationRunner {
    @Resource
    private LicenseVerify licenseVerify;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        LicenseContent content = licenseVerify.install();
    }
}
