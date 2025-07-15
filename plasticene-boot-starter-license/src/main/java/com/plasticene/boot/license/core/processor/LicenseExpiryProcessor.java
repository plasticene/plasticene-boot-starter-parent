package com.plasticene.boot.license.core.processor;

import java.util.Date;

/**
 * @author ZFJ
 * @date 2025/7/15
 */
public interface LicenseExpiryProcessor {

    void warn(Date expiryDate);

}
