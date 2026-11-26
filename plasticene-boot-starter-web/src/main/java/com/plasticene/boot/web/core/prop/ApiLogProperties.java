package com.plasticene.boot.web.core.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2023/10/31 18:55
 */
@Data
@ConfigurationProperties(prefix = "ptc.api.log")
public class ApiLogProperties {
    /**
     * 是否开启 Controller 接口日志打印，默认关闭；开启后可通过 @ApiLogIgnore 排除。
     */
    private Boolean enable = Boolean.FALSE;
}
