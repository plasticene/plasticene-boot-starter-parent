package com.plasticene.boot.web.core.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2023/4/27 15:04
 */

@Data
@ConfigurationProperties(prefix = "ptc.api.security")
public class ApiSecurityProperties {

    /**
     * 是否开启接口安全验证
     */
    private Boolean enable = true;

    /**
     * 签名有效时长，单位：秒
     */
    private Integer validTime = 60;

    /**
     * rsa私钥
     */
    private String rsaPrivateKey;

    /**
     * rsa公钥
     */
    private String rsaPublicKey;
}
