package com.plasticene.boot.license.core;

import com.plasticene.boot.common.constant.OrderConstant;
import com.plasticene.boot.license.core.processor.LicenseContentProcessor;
import de.schlichtherle.license.LicenseContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;

import java.util.Collections;
import java.util.List;


/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/8/3 11:51
 */
@Order(OrderConstant.RUNNER_LICENSE)
public class LicenseCheckApplicationRunner implements ApplicationRunner {
    @Autowired
    private LicenseVerify licenseVerify;
    @Autowired(required = false)
    private List<LicenseContentProcessor> processors = Collections.emptyList();

    @Override
    public void run(ApplicationArguments args) {
        LicenseContent content = licenseVerify.install();
        // 扩展点，在证书安装之后对licenseContent进行业务逻辑处理，比如数据加载，开关判断等等
        // 这里参考了spring的后置处理器，其实我们一般实现一个处理器逻辑处理就可以了，当然了也可以实现多个，所以这里是数组
        processors.forEach(processor -> processor.process(content));
    }
}
