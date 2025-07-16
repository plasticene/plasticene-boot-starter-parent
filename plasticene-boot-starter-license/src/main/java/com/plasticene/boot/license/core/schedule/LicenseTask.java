package com.plasticene.boot.license.core.schedule;

import com.plasticene.boot.license.core.processor.LicenseExpiryProcessor;
import com.plasticene.boot.license.core.LicenseVerify;
import de.schlichtherle.license.LicenseContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Date;
import java.util.Objects;

/**
 * @author ZFJ
 * @date 2025/7/15
 */

@Slf4j
public class LicenseTask {
    @Autowired
    private LicenseVerify licenseVerify;
    @Autowired(required = false)
    private LicenseExpiryProcessor licenseExpiryProcessor;

    /**
     *  校验license是否合法有效
     */
    @Scheduled(cron = "${ptc.license.check-cron:0 30 0 * * ?}")
    public void checkLicenseValidity() {
        try {
            LicenseContent content = licenseVerify.verify();
            Date expiryDate = content.getNotAfter();
            // 业务方可以基于有效期进行提前提醒啥的
            if (Objects.nonNull(licenseExpiryProcessor)) {
                licenseExpiryProcessor.warn(expiryDate);
            }
        } catch (Exception e) {
            log.error("check license failed", e);
        }
    }

    /**
     * license热更新
     */
    @Scheduled(cron = "${ptc.license.refresh-cron:0 0 0/1 * * ?}")
    public void refreshLicense () {
        licenseVerify.refresh();
    }
}
