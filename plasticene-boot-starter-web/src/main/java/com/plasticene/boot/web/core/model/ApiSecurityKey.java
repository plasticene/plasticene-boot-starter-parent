package com.plasticene.boot.web.core.model;

import lombok.Data;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/5/23
 */
@Data
public class ApiSecurityKey {

    /**
     * 我方rsa私钥
     */
    private String rsaPrivateKey;

    /**
     * 我方ras公钥
     */
    private String rsaPublicKey;

    /**
     * 第三方rsa公钥
     */
    private String thirdRsaPublicKey;
}
