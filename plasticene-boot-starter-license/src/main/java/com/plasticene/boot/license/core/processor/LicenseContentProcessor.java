package com.plasticene.boot.license.core.processor;

import de.schlichtherle.license.LicenseContent;

/**
 * 在证书安装之后对licenseContent进行业务逻辑处理，比如数据加载，开关判断等等
 * @author fjzheng
 * @date 2025/7/15
 */
public interface LicenseContentProcessor {

    void process(LicenseContent licenseContent);
}
