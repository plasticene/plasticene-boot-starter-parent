package com.plasticene.boot.web.core.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/14 10:12
 */
@Data
@ConfigurationProperties(prefix = "ptc.trace")
public class TraceProperties {
    /**
     * 是否开启日志链路追踪
     */
    private Boolean enable = true;
}
