package com.plasticene.boot.web.core.advice;

import com.plasticene.boot.web.core.model.ApiSecurityKey;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/5/23
 */
public interface ApiSecurityKeyProvider {

    /**
     * 根据平台标识获取key
     * @param appId 第三方应用id
     * @return 对应平台的加解密key信息
     */
    ApiSecurityKey getApiSecurityKey(String appId);
}
