package com.plasticene.boot.example.license.processor;

import cn.hutool.core.date.DateUtil;
import com.plasticene.boot.license.core.processor.LicenseContentProcessor;
import de.schlichtherle.license.LicenseContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/7/15
 */
@Slf4j
@Component
public class LocalLicenseContentProcessor implements LicenseContentProcessor {
    @Override
    public void process(LicenseContent licenseContent) {
        log.info("license content expiry date: {}, extra: {}",
                DateUtil.formatDate(licenseContent.getNotAfter()),
                licenseContent.getExtra());
    }
}
