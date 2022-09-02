package com.plasticene.boot.mybatis.core.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/9/1 13:40
 */
@ConfigurationProperties(prefix = "ptc.id")
@Data
public class IdProperties {
    private Long datacenter = 0l;
    private Long worker = 0l;
}
