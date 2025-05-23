package com.plasticene.boot.web.core.advice;

import com.plasticene.boot.web.core.model.ApiSecurityKey;
import com.plasticene.boot.web.core.prop.ApiSecurityProperties;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/5/23
 */
public class DefaultApiSecurityKeyProvider implements ApiSecurityKeyProvider {

    private final ApiSecurityProperties properties;

    public DefaultApiSecurityKeyProvider(ApiSecurityProperties properties) {
        this.properties = properties;
    }


    @Override
    public ApiSecurityKey getApiSecurityKey(String appId) {
        ApiSecurityKey apiSecurityKey = new ApiSecurityKey();
        apiSecurityKey.setRsaPrivateKey(properties.getRsaPrivateKey());
        apiSecurityKey.setRsaPublicKey(properties.getRsaPublicKey());
        apiSecurityKey.setThirdRsaPublicKey(properties.getRsaPublicKey());
        return apiSecurityKey;
    }
}
