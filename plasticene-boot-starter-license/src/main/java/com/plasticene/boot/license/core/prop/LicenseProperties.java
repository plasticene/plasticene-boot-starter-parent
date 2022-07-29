package com.plasticene.boot.license.core.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/28 17:55
 */
@ConfigurationProperties(prefix = "ptc.license")
@Data
public class LicenseProperties {
    /**
     * 证书subject
     */
    private String subject = "license-demo";

    /**
     * 公钥别称
     */
    private String publicAlias = "publicCert";

    /**
     * 访问公钥库的密码
     */
    private String storePass = "plasticene666666";

    /**
     * 证书生成路径
     */
    private String licensePath = "/root/license/license.lic";

    /**
     * 密钥库存储路径
     */
    private String publicKeysStorePath = "/root/license/publicCerts.store";


    /**
     * 密钥别称
     */
    private String privateAlias = "privateKey";

    /**
     * 密钥密码（需要妥善保管，不能让使用者知道）
     */
    private String keyPass = "plasticene666666";


    /**
     * 密钥库存储路径
     */
    private String privateKeysStorePath = "/root/license/privateKeys.keystore";


}
