package com.plasticene.boot.license.core;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.license.core.constant.LicenseConstant;
import com.plasticene.boot.license.core.enums.VerifySystemType;
import com.plasticene.boot.license.core.param.CustomKeyStoreParam;
import com.plasticene.boot.license.core.param.SystemInfo;
import com.plasticene.boot.license.core.prop.LicenseProperties;
import com.plasticene.boot.license.core.utils.DmcUtils;
import de.schlichtherle.license.*;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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

    private static final Logger logger = LogManager.getLogger(LicenseVerify.class);

    /**
     * license合法标识
     */
    private final AtomicBoolean licenseValid  = new AtomicBoolean(true);

    /**
     * license文件md5
     */
    private final AtomicReference<String> licenseFileMd5 = new AtomicReference<>(StrUtil.EMPTY);



    /**
     * license是否合法
     * @return true/false
     */
    public boolean isValid() {
        return licenseValid.get();
    }

    /**
     * 安装License证书
     * 项目服务启动时候安装证书，检验合法性
     * 此时根据开关验证服务器系统信息
     */
    public synchronized LicenseContent install() {
        try {
            LicenseManager licenseManager = LicenseManagerHolder.getInstance(initLicenseParam());
            licenseManager.uninstall();
            // 安装license
            LicenseContent licenseContent = licenseManager.install(new File(licenseProperties.getLicensePath()));
            // 校验硬件信息
            verifySystemInfo(licenseContent);
            logger.info("证书安装成功，证书有效期：{} - {}",
                    DateUtil.formatDate(licenseContent.getNotBefore()),
                    DateUtil.formatDate(licenseContent.getNotAfter()));
            licenseValid.set(true);
            String md5 = DigestUtil.md5Hex(new File(licenseProperties.getLicensePath()));
            licenseFileMd5.set(md5);
            return licenseContent;
        } catch (Exception e) {
            logger.error("证书安装失败:", e);
            throw new BizException("证书安装失败");
        }
    }

    /**
     * 校验License证书，此时无需再验证服务器系统信息，验证证书和有效期即可
     */
    public LicenseContent verify() {
        try {
            LicenseManager licenseManager = LicenseManagerHolder.getInstance(initLicenseParam());
            LicenseContent content = licenseManager.verify();
            licenseValid.set(true);
            return content;
        } catch (Exception e) {
            licenseValid.set(false);
            logger.error("证书校验失败:", e);
            throw new BizException("证书检验失败");
        }
    }

    /**
     * 动态刷新license证书
     */
    public synchronized void refresh() {
        String md5 = DigestUtil.md5Hex(new File(licenseProperties.getLicensePath()));
        if (Objects.equals(licenseFileMd5.get(), md5)) {
            // license文件没有更新
            return;
        }
        // license更新了，重新安装证书
        install();
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

    // 验证证书有效期

    /**
     * 验证证书有效期
     * licenseManager.verify()已经验证有效期了，此方法没必要
     */
    private void verifyExpiry(LicenseContent licenseContent) {
        Date expiry = licenseContent.getNotAfter();
        Date current = new Date();
        if (current.after(expiry)) {
            throw new BizException("证书已过期");
        }
    }

    @SuppressWarnings("unchecked")
    private void verifySystemInfo(LicenseContent licenseContent) {
        if (!licenseProperties.getVerifySystemSwitch()) {
            return;
        }
        Map<String, Object> map = (Map<String, Object>)licenseContent.getExtra();
        SystemInfo systemInfo = (SystemInfo) map.get(LicenseConstant.SYSTEM_INFO);
        VerifySystemType verifySystemType = licenseProperties.getVerifySystemType();
        switch (verifySystemType) {
            case CPU_ID -> checkCpuId(systemInfo.getCpuId());
            case SYSTEM_UUID -> checkSystemUuid(systemInfo.getUuid());
        }
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
