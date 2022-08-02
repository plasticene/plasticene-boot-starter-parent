package com.plasticene.boot.license.core;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.license.core.enums.VerifySystemType;
import com.plasticene.boot.license.core.param.CustomKeyStoreParam;
import com.plasticene.boot.license.core.param.SystemInfo;
import com.plasticene.boot.license.core.prop.LicenseProperties;
import com.plasticene.boot.license.core.utils.DmcUtils;
import de.schlichtherle.license.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/28 15:37
 */
@Component
public class LicenseVerify {
    @Resource
    private LicenseProperties licenseProperties;

    private static Logger logger = LogManager.getLogger(LicenseVerify.class);
    private static final  DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    /**
     * 安装License证书
     */
    public synchronized LicenseContent install() {
        LicenseContent result = null;
        try{
            LicenseManager licenseManager = new LicenseManager(initLicenseParam());
            licenseManager.uninstall();
            result = licenseManager.install(new File(licenseProperties.getLicensePath()));
            logger.info("证书安装成功，证书有效期：{} - {}", df.format(result.getNotBefore()),
                    df.format(result.getNotAfter()));
        }catch (Exception e){
            logger.error("证书安装失败:", e);
            throw new BizException("证书安装失败");
        }
        return result;
    }

    /**
     * 校验License证书
     */
    public boolean verify() {
        try {
            LicenseManager licenseManager = new LicenseManager(initLicenseParam());
            LicenseContent licenseContent = licenseManager.verify();
            if (licenseProperties.getVerifySystemSwitch()) {
                SystemInfo systemInfo = (SystemInfo) licenseContent.getExtra();
                VerifySystemType verifySystemType = licenseProperties.getVerifySystemType();
                switch (verifySystemType) {
                    case CPU_ID:
                        checkCpuId(systemInfo.getCpuId());
                        break;
                    case SYSTEM_UUID:
                        checkSystemUuid(systemInfo.getUuid());
                        break;
                    default:
                        checkCpuId(systemInfo.getCpuId());
                }

            }
            logger.info("证书校验通过，证书有效期：{} - {}",df.format(licenseContent.getNotBefore()),
                    df.format(licenseContent.getNotAfter()));
            return true;
        }catch (Exception e){
            logger.error("证书校验失败:",e);
            throw new BizException("证书检验失败");
        }
    }

    /**
     * 初始化证书生成参数
     */
    private LicenseParam initLicenseParam(){
        Preferences preferences = Preferences.userNodeForPackage(LicenseVerify.class);

        CipherParam cipherParam = new DefaultCipherParam(licenseProperties.getStorePass());

        KeyStoreParam publicStoreParam = new CustomKeyStoreParam(LicenseVerify.class
                ,licenseProperties.getPublicKeysStorePath()
                ,licenseProperties.getPublicAlias()
                ,licenseProperties.getStorePass()
                ,null);

        return new DefaultLicenseParam(licenseProperties.getSubject()
                ,preferences
                ,publicStoreParam
                ,cipherParam);
    }

    private void checkCpuId(String cpuId) {
        cpuId = cpuId.trim().toUpperCase();
        String systemCpuId = DmcUtils.getCpuId().trim().toUpperCase();
        logger.info("配置cpuId = {},  系统cpuId = {}", cpuId, systemCpuId);
        if (!Objects.equals(cpuId, systemCpuId)) {
            throw new BizException("license检验cpuId不一致");
        }
    }

    private void checkSystemUuid(String uuid) {
        uuid = uuid.trim().toUpperCase();
        String systemUuid = DmcUtils.getSystemUuid().trim().toUpperCase();
        logger.info("配置uuid = {},  系统uuid= {}", uuid, systemUuid);
        if (!Objects.equals(uuid, systemUuid)) {
            throw new BizException("license检验uuid不一致");
        }
    }

}
