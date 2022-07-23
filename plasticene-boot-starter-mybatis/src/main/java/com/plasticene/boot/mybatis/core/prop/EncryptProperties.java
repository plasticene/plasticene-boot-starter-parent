package com.plasticene.boot.mybatis.core.prop;

import com.plasticene.boot.mybatis.core.enums.Algorithm;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/22 15:43
 */
@ConfigurationProperties(prefix = "ptc.encrypt")
@Data
public class EncryptProperties {
    /**
     * 加密算法 {@link Algorithm}
     */
    private Algorithm algorithm = Algorithm.BASE64;

    /**
     * aes算法需要秘钥key
     */
    private String key = "plasticene666666";

    /**
     * aes算法需要一个偏移量
     */
    private String iv = "plasticene666666";

}
