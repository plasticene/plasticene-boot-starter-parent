package com.plasticene.boot.license.core;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.license.core.constant.LicenseConstant;
import com.plasticene.boot.license.core.param.CustomKeyStoreParam;
import com.plasticene.boot.license.core.param.LicenseCreatorParam;
import com.plasticene.boot.license.core.prop.LicenseProperties;
import de.schlichtherle.license.*;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/28 14:31
 * license使用流程：
 * 1.密钥对生成：开发者生成公钥/私钥对
 * 2.应用集成：将公钥集成到应用中
 * 3.许可证生成：使用私钥生成许可证文件
 * 4.分发：将许可证文件分发给最终用户
 * 5.验证：应用使用公钥验证许可证
 */

public class LicenseCreator {
    @Resource
    private LicenseProperties licenseProperties;

    private static final Logger logger = LogManager.getLogger(LicenseCreator.class);
    private final static X500Principal DEFAULT_HOLDER_AND_ISSUER = new X500Principal("CN=localhost, OU=localhost, O=localhost, L=SH, ST=SH, C=CN");


    /**
     * 生成License证书
     */
    public void generateLicense(LicenseCreatorParam param, File licenseFile) throws IOException {
        try {
            LicenseManager licenseManager = new LicenseManager(initLicenseParam());
            LicenseContent licenseContent = initLicenseContent(param);
            licenseManager.store(licenseContent, licenseFile);
        }catch (Exception e){
            logger.error("证书生成失败：", e);
            throw new BizException("生成license证书失败");
        }
    }

    /**
     * 初始化证书生成参数
     */
    private LicenseParam initLicenseParam(){
        Preferences preferences = Preferences.userNodeForPackage(LicenseCreator.class);
        // 设置对证书内容加密的秘钥
        CipherParam cipherParam = new DefaultCipherParam(licenseProperties.getStorePass());

        KeyStoreParam privateStoreParam = new CustomKeyStoreParam(LicenseCreator.class
                ,licenseProperties.getPrivateKeysStorePath()
                ,licenseProperties.getPrivateAlias()
                ,licenseProperties.getStorePass()
                ,licenseProperties.getKeyPass());

        return new DefaultLicenseParam(licenseProperties.getSubject()
                ,preferences
                ,privateStoreParam
                ,cipherParam);
    }

    /**
     * 设置证书生成正文信息
     */
    private LicenseContent initLicenseContent(LicenseCreatorParam param){
        LicenseContent licenseContent = new LicenseContent();
        licenseContent.setHolder(DEFAULT_HOLDER_AND_ISSUER);
        licenseContent.setIssuer(DEFAULT_HOLDER_AND_ISSUER);

        licenseContent.setSubject(licenseContent.getSubject());
        licenseContent.setIssued(param.getIssuedTime());
        licenseContent.setNotBefore(param.getIssuedTime());
        licenseContent.setNotAfter(param.getExpiryTime() == null ? addYears(new Date(), 10) : param.getExpiryTime());
        licenseContent.setConsumerType(param.getConsumerType());
        licenseContent.setConsumerAmount(param.getConsumerAmount());
        licenseContent.setInfo(param.getDescription());
        Map<String, Object> extra = param.getExtra();
        extra.put(LicenseConstant.SYSTEM_INFO, param.getSystemInfo());
        licenseContent.setExtra(extra);
        return licenseContent;
    }

    public  Date addYears(Date date, int n) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.YEAR, n);
        return cal.getTime();
    }


}
