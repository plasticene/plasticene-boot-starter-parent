package com.plasticene.boot.license.core;

import de.schlichtherle.license.LicenseManager;
import de.schlichtherle.license.LicenseParam;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/28 15:44
 * 双重校验生成LicenseManager单例，但是由于生成和安装、校验的LicenseManager配置不一样
 * 所以并不是全局唯一的，所以单例意义不大，安装和验证可以使用同一个LicenseManager
 */
public class LicenseManagerHolder {

    private static volatile LicenseManager LICENSE_MANAGER;

    public static LicenseManager getInstance(LicenseParam param){
        if(LICENSE_MANAGER == null){
            synchronized (LicenseManagerHolder.class){
                if(LICENSE_MANAGER == null){
                    LICENSE_MANAGER = new LicenseManager(param);
                }
            }
        }
        return LICENSE_MANAGER;
    }

}

