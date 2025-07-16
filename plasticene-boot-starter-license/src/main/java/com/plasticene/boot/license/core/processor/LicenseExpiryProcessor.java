package com.plasticene.boot.license.core.processor;

import java.util.Date;

/**
 * license即将到期逻辑处理，如提醒警告
 * @author ZFJ
 * @date 2025/7/15
 */
public interface LicenseExpiryProcessor {

    void warn(Date expiryDate);

}
