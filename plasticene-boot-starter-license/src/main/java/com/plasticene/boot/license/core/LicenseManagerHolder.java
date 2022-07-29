package com.plasticene.boot.license.core;

import de.schlichtherle.license.LicenseManager;
import de.schlichtherle.license.LicenseParam;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/28 15:44
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

