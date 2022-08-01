package com.plasticene.boot.license.core;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.license.core.param.CustomKeyStoreParam;
import com.plasticene.boot.license.core.param.LicenseCreatorParam;
import com.plasticene.boot.license.core.prop.LicenseProperties;
import de.schlichtherle.license.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.util.prefs.Preferences;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/28 14:31
 */
@Component
public class LicenseCreator {
    @Resource
    private LicenseProperties licenseProperties;

    private static Logger logger = LogManager.getLogger(LicenseCreator.class);
    private final static X500Principal DEFAULT_HOLDER_AND_ISSUER = new X500Principal("CN=localhost, OU=localhost, O=localhost, L=SH, ST=SH, C=CN");


    /**
     * 生成License证书
     */
    public boolean generateLicense(LicenseCreatorParam param){
        try {
            LicenseManager licenseManager = new LicenseManager(initLicenseParam());
            LicenseContent licenseContent = initLicenseContent(param);
            licenseManager.store(licenseContent, new File(licenseProperties.getLicensePath()));
            return true;
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

        //设置对证书内容加密的秘钥
        CipherParam cipherParam = new DefaultCipherParam(licenseProperties.getStorePass());

        KeyStoreParam privateStoreParam = new CustomKeyStoreParam(LicenseCreator.class
                ,licenseProperties.getPrivateKeysStorePath()
                ,licenseProperties.getPrivateAlias()
                ,licenseProperties.getStorePass()
                ,licenseProperties.getKeyPass());

        LicenseParam licenseParam = new DefaultLicenseParam(licenseProperties.getSubject()
                ,preferences
                ,privateStoreParam
                ,cipherParam);

        return licenseParam;
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
        licenseContent.setNotAfter(param.getExpiryTime());
        licenseContent.setConsumerType(param.getConsumerType());
        licenseContent.setConsumerAmount(param.getConsumerAmount());
        licenseContent.setInfo(param.getDescription());

//        licenseContent.setExtra(param.getLicenseCheckModel());

        return licenseContent;
    }

}
